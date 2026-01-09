package bshell.modules;

import bshell.configs.ConfigManager;
import bshell.database.DatabaseService;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NucleiModule extends AbstractModule {

    // Mapping avec gson
    private static class NucleiResult {
        // on demande à gson d'aller chercher la clé template-id et on met la valeur dans templateId
        @SerializedName("template-id") String templateId; 
        @SerializedName("matched-at") String matchedAt;
        Info info;
    }
    private static class Info {
        String name;
        String severity;
    }

    private final Gson gson = new Gson();
    
    
    // Couleurs ANSI 
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREY = "\u001B[90m";
    private static final String BOLD = "\u001B[1m";

    // Constructeur
    public NucleiModule() {
        super("nuclei", "Scanner de vulnérabilités Web");

        // Comme NucleiModule hérite de AbstractModule alors il a accès au protected registerOption
        registerOption("RHOST", "http://127.0.0.1", "URL web", true);
        registerOption("THREADS", "150", "Concurrence", false);
        registerOption("RATE_LIMIT", "2000", "Requêtes/sec", false);
    }

    @Override
    public void run() throws ModuleExecutionException {
        String target = getOption("RHOST").orElseThrow(() -> new ModuleExecutionException("TARGET non défini !")); // Ici on utilise le premier constructeur car msg simple
        String threads = getOption("THREADS").orElse("150");
        String rateLimit = getOption("RATE_LIMIT").orElse("2000");

        System.out.println("[*] Démarrage de Nuclei sur " + target);

        // Singleton, on n'appelle pas le constructeur mais la méthode qui initie le constructeur
        DatabaseService dbService = DatabaseService.getInstance();

        List<NucleiResult> findings = new ArrayList<>();

        try {
            StringBuilder nucleiCmd = new StringBuilder();
            String nucleiBinary = ConfigManager.getInstance().getBinaryPath("nuclei");
            nucleiCmd.append(nucleiBinary);
            nucleiCmd.append(" -target ").append(target);
            nucleiCmd.append(" -c ").append(threads);
            nucleiCmd.append(" -rl ").append(rateLimit);
            nucleiCmd.append(" -timeout 2");
            nucleiCmd.append(" -ni -nm -j"); // des params pour opti et output json
            
            /*
            Source : https://jvns.ca/blog/2024/11/29/why-pipes-get-stuck-buffering/

            Explication : 
            Linux cherche à optimiser les performances. Ainsi, lorsqu’on lance une commande dans un terminal lambda, 
            il ne cherche pas à optimiser la manière de transporter les informations entre les différents pipes ou sorties.
            En revanche, entre programmes (donc dans notre ProcessBuilder), il va mettre les données en mémoire tampon (bufferiser), prendre son temps, puis tout relâcher d’un seul coup.

            Conséquences : Le résultat est toujours le même mais la performance est grandement impacté...

            Solution : script permet de simuler un terminal TTY et donc éviter les optimisation linux
            
            */
            ProcessBuilder pb;
            if (System.getProperty("os.name").toLowerCase().contains("linux")) {

                List<String> wrapper = new ArrayList<>();
                
                wrapper.add("script");
                wrapper.add("-q"); 
                wrapper.add("-c");
                wrapper.add(nucleiCmd.toString()); 
                wrapper.add("/dev/null");

                pb = new ProcessBuilder(wrapper);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                try(reader) {

                    String line;

                    // On commence le parsing des logs et json
                    while ((line = reader.readLine()) != null) {
                        /* Explication de regex dans le replaceAll
                        Dans les terminales, on peut mettre de la couleur ANSI comme je fais.
                        Nuclei ne fait pas exception et du coup par précausition on les enlève.

                        Fonctionnement :
                        - Chaque couleur ANSI commence par "\u001B"
                        - Ensuite [ est considéré comme un caractère spéciale regex du coup on l'échappe avec \\
                        - [;\\d]* : \d -> chiffre et le * c'est autant de fois
                        - m C'est toujours la lettre de fin pour une couleur ANSI

                        */
                        String cleanLine = line.replaceAll("\u001B\\[[;\\d]*m", "").trim();

                        // Si la ligne commence avec "{" c'est du json.
                        if (cleanLine.startsWith("{")) {
                            try {
                                NucleiResult record = gson.fromJson(cleanLine, NucleiResult.class);
                                findings.add(record); 
                            } catch (Exception e) {
                                System.out.println(GREY + "   [ERR JSON] " + e.getMessage() + RESET);
                            }
                        } 
                        // Debug
                        else if (!cleanLine.isEmpty()) {
                            System.out.println(GREY + "   [LOG] " + cleanLine + RESET);
                        }
                    }
                    process.waitFor();
                
                    if (!findings.isEmpty()) {
                        printSummaryTable(findings);
                        askToSave(findings, dbService);
                    } else {
                        System.out.println("\n" + GREEN + "[-] Scan terminé. Aucune vulnérabilité trouvée." + RESET);
                    }
                }
            } else {
                new ModuleExecutionException("OS non compatible, veuillez utiliser Linux !");
            }

        } catch (Exception e) {
            throw new ModuleExecutionException("Erreur: " + e.getMessage(), e);
        }
    }

    private void askToSave(List<NucleiResult> findings, DatabaseService dbService) {
        System.out.println(YELLOW + "Voulez-vous sauvegarder ces " + findings.size() + " résultats en base de données ? (y/N)" + RESET);
        System.out.print("> ");
        
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine().trim();

        boolean isYes = input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("oui");

        if (!isYes) {
            System.out.println("[-] Sauvegarde annulée.");
            return; 
        }

        System.out.println("Sauvegarde en cours...");
        int count = 0;
        
        for (NucleiResult v : findings) {
            try {
                String vulnName;
                
                if (v.info != null && v.info.name != null) {
                    vulnName = v.info.name;
                } else {
                    vulnName = v.templateId;
                }

                String jsonContent = gson.toJson(v);
                
                dbService.saveVulnerability(vulnName, v.matchedAt, "OPEN", jsonContent);
                count++;
                
            } catch (Exception e) {
                System.out.println(RED + "[!] Erreur sauvegarde item: " + e.getMessage() + RESET);
            }
        }
        
        System.out.println(GREEN + "[+] " + count + " vulnérabilités sauvegardées." + RESET);
        // ne pas close le scanner car sinon jline crashera aussi...
    }

    private void printSummaryTable(List<NucleiResult> findings) {
        System.out.println("\n=== SYNTHÈSE DU SCAN ===");
        String format = "| %-10s | %-40s | %-25s | %-30s |%n";
        printSeparator();
        System.out.format(format, "SÉVÉRITÉ", "NOM", "TEMPLATE ID", "ENDPOINT");
        printSeparator();

        for (NucleiResult v : findings) {
            String sev = (v.info != null && v.info.severity != null) ? v.info.severity : "unknown";
            String rawName = (v.info != null && v.info.name != null) ? v.info.name : "N/A";
            String rawId = (v.templateId != null) ? v.templateId : "N/A";
            String rawUrl = (v.matchedAt != null) ? v.matchedAt : "N/A";

            String sevDisplay = getSeverityColor(sev) + sev.toUpperCase() + RESET;
            
            System.out.format("| %-10s | %-40s | %-25s | %-30s |%n", 
                sevDisplay + getPadding(sev, 10),
                truncate(rawName, 38), truncate(rawId, 23), truncate(rawUrl, 28));
        }
        printSeparator();
        System.out.println("Total : " + findings.size() + " vulnérabilités.\n");
    }

    private void printSeparator() { System.out.println("+------------+------------------------------------------+---------------------------+--------------------------------+"); }
    
    // DRY
    private String getSeverityColor(String severity) {
        String s = severity.toUpperCase();
        if (s.contains("CRITICAL") || s.contains("HIGH")) return RED + BOLD;
        if (s.contains("MEDIUM")) return YELLOW;
        if (s.contains("LOW")) return BLUE;
        return CYAN;
    }
    
    private String getPadding(String str, int expectedWidth) {
        if (str == null) {
            return " ".repeat(expectedWidth);
        }
        int padding = expectedWidth - str.length();
        return " ".repeat(Math.max(0, padding));
    }

    private String truncate(String str, int width) {
        if (str.length() > width) {
            return str.substring(0, width - 3) + "...";
        }
        return str;
    }
}