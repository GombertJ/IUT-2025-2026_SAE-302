package bshell;

import bshell.configs.Config;
import bshell.configs.ConfigManager;
import bshell.shell.ShellService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "bshell", mixinStandardHelpOptions = true, version = "bshell 0.1", description = "Bowser Shell - A shell for automating pentesting tasks.")
public class BShell implements Runnable {

    /*
    Jline utilise @Option qui sont des interfaces, 
    pour nous c'est les options de la commande.
    */

    @Option(names = {"-d", "--dbPath"}, description = "path")
    String dbPath;
    
    @Option(names = {"-D", "--directory"}, description = "Working directory for the shell modules.")
    String workingDirectory;

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final Config config = configManager.getConfig();

    @Override
    public void run() {
        System.out.println("BShell is running. Use --help for more information.");

        if (this.dbPath != null || this.workingDirectory != null) {
            if (this.dbPath != null) {
                this.config.dbPath = this.dbPath;
            }

            if (this.workingDirectory != null) {
                this.config.workspacePath = this.workingDirectory;
            }

            this.configManager.save();
        }

        new Setup();
        ShellService shellService = new ShellService();
        shellService.start();
    }

    public static void main(String[] args) {
        new CommandLine(new BShell()).execute(args);
    }
}
