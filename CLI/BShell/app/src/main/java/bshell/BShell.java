package bshell;

import bshell.shell.ShellService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

// Runnable est une instance d'une classe pouvant être exécuté par un thread
@Command(name = "bshell", mixinStandardHelpOptions = true, version = "bshell 0.1",
        description = "Bowser Shell - A shell for automating pentesting tasks.")
public class BShell implements Runnable {

    // Les options

    @Option(names = {"-d", "--dbPath"}, description = "path")
    String dbPath;
    
    @Option(names = {"-D", "--directory"}, description = "Working directory for the shell modules.")
    String workingDirectory;

    // Override est une instruction qui dit au compiler que je vais modifier une méthode appartenant à la super classe
    @Override
    public void run() {
        System.out.println("BShell is running. Use --help for more information.");

        new Setup();
        ShellService shellService = new ShellService();
        shellService.start();
    }

    public static void main(String[] args) {
        new CommandLine(new BShell()).execute(args);
        
    }
}
