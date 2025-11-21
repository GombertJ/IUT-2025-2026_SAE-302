package bshell;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

// Runnable describes a class whose instances can be executed by a thread. 
@Command(name = "bshell", mixinStandardHelpOptions = true, version = "bshell 0.1",
        description = "Bowser Shell - A shell for automating pentesting tasks.")
public class BShell implements Runnable {

    // Option of the command //

    //@TODO: Add more options as needed.
    @Option(names = {"-d", "--database"}, description = "Nework database ip address.")
    String databaseIP;

    @Option(names = {"-u", "--user"}, description = "Username for database authentication.")
    String username;

    @Option(names = {"-p", "--password"}, description = "Password for database authentication.")
    String password;
    
    @Option(names = {"-D", "--directory"}, description = "Working directory for the shell modules.")
    String workingDirectory;

    // Override instruct the compiler that i intend to override a method in a superclass. (Runnable) 
    @Override
    public void run() {
        System.out.println("BShell is running. Use --help for more information.");

        Setup setup = new Setup();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BShell()).execute(args);
        System.exit(exitCode);
    }
}
