package bshell.modules;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class NmapModule extends AbstractModule {

    public NmapModule() {
        super("nmap", "Scanner réseau utilisant Nmap");
        // On définit les options classiques de Nmap
        registerOption("TARGET", "127.0.0.1", "Adresse IP ou domaine cible", true);
        registerOption("PORTS", "", "Ports à scanner (ex: 80,443). Vide = 1000 ports par défaut", false);
        registerOption("ARGS", "-sV", "Arguments supplémentaires nmap", false);
    }

    @Override
    public void run() throws ModuleExecutionException {
        String target = getOption("TARGET").orElseThrow(() -> new ModuleExecutionException("TARGET non défini !"));
        String ports = getOption("PORTS").orElse("");
        String args = getOption("ARGS").orElse("");

        System.out.println("[*] Démarrage de Nmap sur " + target + "...");

        try {
            // Construction de la commande système
            List<String> command = new ArrayList<>();
            command.add("nmap"); // Nmap doit être installé sur le système (Setup s'en chargera)
            
            if (!args.isEmpty()) {
                String[] argParts = args.split(" ");
                for(String arg : argParts) command.add(arg);
            }
            
            if (!ports.isEmpty()) {
                command.add("-p");
                command.add(ports);
            }
            
            command.add(target);

            // Exécution réelle via ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Fusionne stdout et stderr
            Process process = pb.start();

            // Lecture du résultat ligne par ligne
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("[+] Scan terminé avec succès.");
            } else {
                throw new ModuleExecutionException("Nmap a quitté avec le code erreur : " + exitCode);
            }

        } catch (Exception e) {
            throw new ModuleExecutionException("Erreur lors de l'exécution de Nmap: " + e.getMessage(), e);
        }
    }
}