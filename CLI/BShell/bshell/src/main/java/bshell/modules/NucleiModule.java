package bshell.modules;

import bshell.configs.ConfigManager;
import bshell.database.DatabaseService;
import bshell.database.Vulnerability;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
                
                    // on lance la synchro (Diff) même si findings est vide pour fermer les vulnérabilités disparues
                    syncVulnerabilities(target, findings, dbService);
                }
            } else {
                new ModuleExecutionException("OS non compatible, veuillez utiliser Linux !");
            }

        } catch (Exception e) {
            throw new ModuleExecutionException("Erreur: " + e.getMessage(), e);
        }
    }

    private void syncVulnerabilities(String targetHost, List<NucleiResult> currentFindings, DatabaseService dbService) {
        System.out.println("\n" + BOLD + "=== SYNCHRONISATION (DIFF) ===" + RESET);
        
        // Historique
        List<Vulnerability> knownVulns = dbService.getVulnsForTarget(targetHost);
        List<String> seenInCurrentScanKeys = new ArrayList<>();
        
        System.out.println("+----------+------------+------------------------------------------+---------------------------+");
        System.out.format("| %-8s | %-10s | %-40s | %-25s |%n", "STATUS", "SEVERITY", "NOM", "ENDPOINT");
        System.out.println("+----------+------------+------------------------------------------+---------------------------+");

        // Gérer les différentes situations (existante, nouvelle et ancienne)
        // && : ET/AND
        for (NucleiResult finding : currentFindings) {
            String name = (finding.info != null && finding.info.name != null) ? finding.info.name : finding.templateId;
            String sev = (finding.info != null && finding.info.severity != null) ? finding.info.severity : "n/a";
            // String url = finding.matchedAt; // On va juste mettre target car les url générés par nuclei sont trop long
            
            String uniqueKey = name + "||" + targetHost;
            seenInCurrentScanKeys.add(uniqueKey);

            Vulnerability existing = findVulnerability(knownVulns, name, targetHost);

            if (existing == null) {
                // Cas : nouveau
                try {
                    String json = gson.toJson(finding);
                    dbService.saveVulnerability(name, targetHost, "open", json);
                    printDiffRow("NEW", sev, name, targetHost, GREEN);
                } catch (Exception e) {
                    System.out.println(RED + "[ERR] Save failed: " + name + RESET);
                }
            } else {
                if ("closed".equals(existing.getState())) {
                    // Cas : Close -> Open
                    dbService.updateState(existing.getId(), "open");
                    printDiffRow("REOPEN", sev, name, targetHost, YELLOW);
                } else {
                    // Cas : inchangé
                    printDiffRow("SAME", sev, name, targetHost, GREY);
                }
            }
        }

        // Gestion des anciennes CVE
        for (Vulnerability v : knownVulns) {
            if (!"open".equals(v.getState())) continue;

            String dbKey = v.getName() + "||" + v.getTarget();

            if (!seenInCurrentScanKeys.contains(dbKey)) {
                String oldSev = "unknown";
                try {
                    NucleiResult oldRes = gson.fromJson(v.getInfos(), NucleiResult.class);
                    if (oldRes.info != null) oldSev = oldRes.info.severity;
                } catch (Exception e) { /* Ignored */ }

                dbService.updateState(v.getId(), "closed");
                printDiffRow("FIXED", oldSev, v.getName(), v.getTarget(), RED);
            }
        }
        
        System.out.println("+----------+------------+------------------------------------------+---------------------------+");
        System.out.println(BOLD + "=== SYNC TERMINÉE ===\n" + RESET);
    }


    private void printDiffRow(String status, String severity, String name, String target, String colorCode) {
        // On tronque les textes trop longs pour ne pas casser le tableau
        String tName = truncate(name, 38);
        String tTarget = truncate(target, 23);
        
        // Astuce : On applique la couleur sur toute la ligne, mais on reset à la fin de chaque cellule 
        // ou à la fin de la ligne pour éviter que la bordure "|" ne prenne la couleur.
        System.out.format("| %s%-8s%s | %s%-10s%s | %s%-40s%s | %s%-25s%s |%n", 
            colorCode, status, RESET,
            colorCode, severity, RESET,
            colorCode, tName, RESET,
            colorCode, tTarget, RESET
        );
    }

    private Vulnerability findVulnerability(List<Vulnerability> list, String name, String target) {
        for (Vulnerability v : list) {
            if (v.getName().equals(name) && v.getTarget().equals(target)) {
                return v;
            }
        }
        return null;
    }
    
    private String truncate(String str, int width) {
        if (str.length() > width) {
            return str.substring(0, width - 3) + "...";
        }
        return str;
    }
}