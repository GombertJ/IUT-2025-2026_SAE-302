package bshell.shell;

import bshell.configs.Config;
import bshell.configs.ConfigManager;
import bshell.modules.Module;
import bshell.modules.ModuleExecutionException;
import bshell.modules.ModuleManager;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Random;

public class ShellService {

    private LineReader reader;
    private final ModuleManager moduleManager;
    private Module currentModule = null; // Le module actuellement sélectionné, important pour savoir le contexte
    private final ConfigManager configManager = ConfigManager.getInstance();
    private final Config config = configManager.getConfig();
    
    // Couleurs ANSI
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String BOLD = "\u001B[1m";

    // Fun Facts (générée par Gemini car c'est fun, j'ai découvert de nouvelles choses CREEPER !)
    private static final String[] FUN_FACTS = {
        "Le premier virus, 'Creeper' (1971), affichait simplement : 'I'm the creeper, catch me if you can!'",
        "Le premier 'bug' informatique (1947) était littéralement un papillon de nuit coincé dans un relais du Mark II.",
        "Le code de lancement des missiles nucléaires US a été '00000000' pendant 20 ans. Très sécurisé.",
        
        "Une requête SQL entre dans un bar, va voir deux tables et demande : 'Puis-je me joindre à vous ?'",
        "sudo rm -rf / : La commande magique pour transformer ton PC en brique décorative.",
        "L'unique utilité d'Internet Explorer était de télécharger Google Chrome.",
        
        "Bowser Shell : Parce que Metasploit était trop lourd à installer.",
        "Si ce shell plante, c'est une fonctionnalité non documentée, pas un bug."    
    };

    /*
    Constructeur
    */
    public ShellService() {
        this.moduleManager = new ModuleManager(); // Charge les modules
        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();
            this.reader = LineReaderBuilder.builder().terminal(terminal).build();
        } catch (IOException e) {
            System.err.println("Impossible d'initialiser JLine : " + e.getMessage());
            System.exit(1);
        }
    }

    /*
    Méthode : C'est la boucle infinie 
    */
    public void start() {
        printRandomBanner();
        printVersionInfo();
        printFunFact();

        while (true) {
            try {
                // En fonction du contexte on change le prompt. Par exemple si on charge nmap
                String prompt = getPrompt();
                String line = reader.readLine(prompt);
                handleUserInput(line);

            } catch (UserInterruptException e) {
                // CTRL+c : Car c'est plus pratique !
                if (currentModule != null) {
                    System.out.println(YELLOW + "\n[!] Retour au menu principal." + RESET);
                    currentModule = null;
                }
            } catch (EndOfFileException e) {
                System.out.println(YELLOW + "\n[*] Arrêt du système..." + RESET);
                return;
            } catch (Exception e) {
                System.out.println(RED + "[-] Erreur critique shell: " + e.getMessage() + RESET);
            }
        }
    }

    /*
    Méthode : afficher le prompt
    */
    private String getPrompt() {
        if (currentModule == null) {
            return BOLD + "bshell > " + RESET;
        } else {
            return BOLD + "bshell (" + RED + currentModule.getName() + RESET + BOLD + ") > " + RESET;
        }
    }

    /*
    Méthode : Gérer les commandes
    */
    private void handleUserInput(String line) {
        if (line == null || line.trim().isEmpty()) return;

        String[] parts = line.trim().split("\\s+"); // Expression regex sous java qui match un ou plusieurs espaces
        String command = parts[0].toLowerCase();

        switch (command) {
            case "exit":
            case "quit":
                if (currentModule != null) {
                    currentModule = null;
                    break;
                } else {
                    throw new EndOfFileException();
                }

            case "clear":
            case "cls":
                reader.getTerminal().puts(InfoCmp.Capability.clear_screen); // Reset le screen
                reader.getTerminal().puts(InfoCmp.Capability.cursor_home); // Place le curseur en haut comme sur linux
                reader.getTerminal().flush(); // C'est pour appliqué les modifications
                break;

            case "help":
            case "?":
                printHelp();
                break;

            case "list":
            case "show":
                if (parts.length > 1 && parts[1].equals("options")) {
                    if (currentModule != null) // Si on a une seul instruction pas besoin des {}. Voir https://www.jmdoudoux.fr/accueil.html
                    currentModule.showOptions(System.out);
                } else {
                    listModules();
                }
                break;

            case "use":
                if (parts.length < 2) {
                    System.out.println(RED + "[-] Usage: use <module_name>" + RESET);
                } else {
                    Optional<Module> m = moduleManager.get(parts[1]);
                    if (m.isPresent()) {
                        currentModule = m.get();
                        System.out.println(GREEN + "[+] Module " + parts[1] + " chargé." + RESET);
                    } else {
                        System.out.println(RED + "[-] Module introuvable : " + parts[1] + RESET);
                    }
                }
                break;


            case "set":
                if (currentModule == null) {
                    System.out.println(RED + "[-] Aucun module chargé. Utilisez 'use <module>'." + RESET);
                    return;
                }
                if (parts.length < 3) {
                    System.out.println(RED + "[-] Usage: set <OPTION> <VALEUR>" + RESET);
                    return;
                }
                try {
                    String key = parts[1];
                    // Reconstituer la valeur si elle contient des espaces
                    String value = line.substring(line.indexOf(parts[2]));
                    currentModule.setOption(key, value);
                    System.out.println(key.toUpperCase() + " => " + value);
                } catch (IllegalArgumentException e) {
                    System.out.println(RED + "[-] Option invalide: " + e.getMessage() + RESET);
                }
                break;

            case "run":
                if (currentModule == null) {
                    System.out.println(RED + "[-] Aucun module chargé." + RESET);
                } else {
                    try {
                        currentModule.run();
                    } catch (ModuleExecutionException e) {
                        System.out.println(RED + "[-] Echec de l'exécution: " + e.getMessage() + RESET);
                    }
                }
                break;

            case "info":
                if (currentModule != null) {
                    System.out.println(currentModule.info());
                } else {
                    System.out.println("Bowser Shell v" + config.version + " - Java Pentesting Framework");
                }
                break;

            default:
                System.out.println(RED + "[-] Commande inconnue: " + command + RESET);
        }
    }

    /*
    Méthode : Afficher l'aide
    */
    private void printHelp() {
        if (currentModule == null){
            System.out.println("\nCommandes Core\n==============");
            System.out.printf("%-20s %s%n", "help", "Affiche ce message");
            System.out.printf("%-20s %s%n", "list", "Liste les modules disponibles");
            System.out.printf("%-20s %s%n", "use <module>", "Sélectionne un module");
            System.out.printf("%-20s %s%n", "exit", "Quitte l'application");
            System.out.printf("%-20s %s%n", "clear", "Efface l'écran");
        } else {
            System.out.println("\nCommandes Module (" + currentModule.getName() + ")\n==================");
            System.out.printf("%-20s %s%n", "show options", "Affiche les options du module");
            System.out.printf("%-20s %s%n", "set <opt> <val>", "Définit une option");
            System.out.printf("%-20s %s%n", "run", "Lance le module");
            System.out.printf("%-20s %s%n", "exit", "Désélectionne le module");
        }
        System.out.println();
    }

    /*
    Méthode : Afficher l'aide
    */
    private void listModules() {
        System.out.println("\nModules disponibles\n===================");
        for (Module m : moduleManager.list()) {
            System.out.printf("%-20s %s%n", m.getName(), m.getDescription());
        }
        System.out.println();
    }

    /*
    C'est les méthodes fun exécuté par la méthode start
    */
    private void printVersionInfo() {
        long count = moduleManager.list().size();
        System.out.println("\n       =[ " + YELLOW + "bowser-shell " + config.version + "-dev" + RESET + " ]");
        System.out.println("+ -- --=[ " + GREEN + count + " modules loaded" + RESET + " ]");
        System.out.println("+ -- --=[ " + RED + "Designé par la Bowser Team" + RESET + " ]");
    }

    private void printFunFact() {
        String fact = FUN_FACTS[new Random().nextInt(FUN_FACTS.length)];
        System.out.println("\n" + YELLOW + "[?] Fun Fact: " + RESET + fact + "\n");
    }

    private void printRandomBanner() {
        // Lit depuis le dossier data/ascii généré par Setup
        File folder = new File(config.workspacePath,"ascii");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files != null && files.length > 0) {
            File randomFile = files[new Random().nextInt(files.length)];
            try {
                Files.lines(randomFile.toPath())
                     .forEach(line -> System.out.println(BLUE + line + RESET));
            } catch (IOException e) {
                System.out.println(RED + "Erreur de lecture bannière" + RESET);
            }
        } else {
            System.out.println(YELLOW + "Bannières introuvables dans data/ascii/" + RESET);
        }
    }
}