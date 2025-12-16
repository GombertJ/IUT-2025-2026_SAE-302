package bshell.modules;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NucleiModule extends AbstractModule {

    public NucleiModule() {
        super("nuclei", "Scanner de vulnérabilités Web rapide utilisant Nuclei");
        // Nuclei travaille souvent sur des URL complètes
        registerOption("TARGET", "http://127.0.0.1", "URL cible (ex: http://example.com)", true);
        // Les tags permettent de filtrer (ex: cve, panel, exposure...)
        registerOption("TAGS", "", "Filtres par tags (ex: cve,misconfig). Vide = tout scanner.", false);
        registerOption("ARGS", "", "Arguments supplémentaires Nuclei", false);
    }

    @Override
    public void run() throws ModuleExecutionException {
        String target = getOption("TARGET").orElseThrow(() -> new ModuleExecutionException("TARGET non défini !"));
        String tags = getOption("TAGS").orElse("");
        String args = getOption("ARGS").orElse("");

        System.out.println("[*] Démarrage de Nuclei sur " + target + "...");
        System.out.println("[*] Les résultats s'afficheront en temps réel.\n");
        System.out.println("=== RAPPORT NUCLEI ===");

        try {
            List<String> command = new ArrayList<>();
            command.add("nuclei"); // Nuclei doit être installé et dans le PATH (ou via le lien symbolique créé par Setup)
            
            command.add("-u");
            command.add(target);

            // On demande du JSON pour pouvoir parser proprement
            command.add("-json");
            // Mode silencieux pour ne pas avoir la bannière ASCII de Nuclei qui polluerait
            command.add("-silent");

            if (!tags.isEmpty()) {
                command.add("-tags");
                command.add(tags);
            }

            if (!args.isEmpty()) {
                // Gestion basique des arguments supplémentaires
                for(String arg : args.split(" ")) command.add(arg);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Rediriger stderr vers stdout pour capturer les erreurs aussi
            Process process = pb.start();

            boolean vulnFound = false;

            // Lecture en streaming (ligne par ligne)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Nuclei renvoie une ligne JSON par vulnérabilité trouvée
                    if (line.startsWith("{")) {
                        parseAndDisplayJsonLine(line);
                        vulnFound = true;
                    } else {
                        // Cas d'erreur ou message informatif non JSON
                        // On peut choisir de l'ignorer ou de l'afficher en gris/debug
                        // System.out.println("[RAW] " + line);
                    }
                }
            }

            int exitCode = process.waitFor();
            
            System.out.println("----------------------------------------");
            if (exitCode == 0) {
                if (!vulnFound) {
                    System.out.println("[-] Aucune vulnérabilité détectée avec les filtres actuels.");
                } else {
                    System.out.println("[+] Scan Nuclei terminé.");
                }
            } else {
                throw new ModuleExecutionException("Nuclei a quitté avec le code erreur : " + exitCode);
            }

        } catch (Exception e) {
            throw new ModuleExecutionException("Erreur lors de l'exécution de Nuclei: " + e.getMessage(), e);
        }
    }

    /**
     * Parse une ligne JSON de Nuclei manuellement avec Regex.
     * Évite d'avoir besoin de dépendances lourdes comme Jackson/Gson.
     */
    private void parseAndDisplayJsonLine(String jsonLine) {
        // Extraction des champs clés
        String templateId = extractJsonValue(jsonLine, "template-id");
        String name = extractJsonValue(jsonLine, "name");
        String severity = extractJsonValue(jsonLine, "severity");
        String matchedAt = extractJsonValue(jsonLine, "matched-at");
        String description = extractJsonValue(jsonLine, "description");

        // Affichage formaté
        System.out.println("----------------------------------------");
        System.out.println("  [!] Faille : " + (name.equals("N/A") ? templateId : name));
        System.out.println("      Sévérité : " + severity.toUpperCase());
        System.out.println("      URL      : " + matchedAt);
        
        if (!templateId.equals("N/A")) {
            System.out.println("      ID       : " + templateId);
        }
        
        // La description est parfois longue, on la coupe si besoin ou on l'affiche telle quelle
        if (!description.equals("N/A") && description.length() > 100) {
             System.out.println("      Info     : " + description.substring(0, 97) + "...");
        } else if (!description.equals("N/A")) {
             System.out.println("      Info     : " + description);
        }
        System.out.println("");
    }

    /**
     * Utilitaire Regex pour extraire une valeur d'une clé JSON simple.
     * Fonctionne pour les structures "key": "value" ou "key":"value".
     */
    private String extractJsonValue(String json, String key) {
        // Pattern: cherche "key" suivi de : suivi de "valeur"
        // Attention: Ce regex est simplifié et ne gère pas les objets imbriqués complexes,
        // mais suffit pour la structure plate des logs Nuclei.
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "N/A";
    }
}