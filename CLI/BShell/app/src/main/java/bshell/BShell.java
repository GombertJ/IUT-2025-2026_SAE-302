package bshell;

import bshell.shell.ShellService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

// Runnable describes a class whose instances can be executed by a thread. 
@Command(name = "bshell", mixinStandardHelpOptions = true, version = "bshell 0.1",
        description = "Bowser Shell - A shell for automating pentesting tasks.")
public class BShell implements Runnable {

    // Option of the command //

    //@TODO: Add more options as needed.
    @Option(names = {"-d", "--dbPath"}, description = "path")
    String dbPath;
    
    @Option(names = {"-D", "--directory"}, description = "Working directory for the shell modules.")
    String workingDirectory;

    // Override instruct the compiler that i intend to override a method in a superclass. (Runnable) 
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
