package bshell;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class Setup {
    
    // Variables 
    String PATH = "data";
    String STAR = "[\u2736] ";
    String TAB = "   ";

    public Setup() {
        System.out.println();
        System.out.println(STAR + "Setting up BShell environment...");

        // Ordre des étapes est importantes ! (Ne pas inverser)
        createWorkingDirectory(PATH);
        createConfigFile();
        extractDefaultAsciiArt();

        // Installer les outils
        checkAndInstallDependencies();

        System.out.println(STAR + "Setup complete.");
        System.out.println();
    }

    private void createWorkingDirectory(String path) {
        File data = new File(path);
        File ascii = new File(path + "/ascii");
        File bin =  new File(path + "/bin");

        try {
            if (!data.exists()) {
                data.mkdirs();
                System.out.println(STAR + "Working directory created at: " + path);
            }
            if (!ascii.exists()) {
                ascii.mkdir();
                System.out.println(STAR + "ASCII directory created at: " + path + "/ascii");
            }
            if (!bin.exists()) {
                bin.mkdir();
                System.out.println(STAR + "Binary directory created at: " + path + "/bin");
            }
        } catch (Exception e) {
            System.out.println("Error creating working directories: " + e.getMessage());
        }
    }

    private void createConfigFile() {
        File config = new File(PATH + "/config.json");
        try {
            if (config.createNewFile()) {
                try (FileWriter writer = new FileWriter(config)) {
                    writer.write("{\n  \"version\": \"0.1\",\n  \"theme\": \"default\"\n}");
                }
                System.out.println(STAR + "Configuration file created at: " + PATH + "/config.json");
            }
        } catch (Exception e) {
            System.out.println("Error creating configuration file: " + e.getMessage());
        }
    }

    /**
     * Copie les fichiers ressources internes vers le dossier externe data/ascii
     */
    private void extractDefaultAsciiArt() {
        System.out.println(STAR + "Extracting default resources...");

        // Liste manuelle des fichiers présents dans src/main/resources/ASCII
        String[] defaultFiles = {
            "Bowser.txt", 
            "maskass.txt", 
            "rammus.txt",
            "creeper.txt"
        };

        for (String filename : defaultFiles) {
            copyResourceToDisk("/ASCII/" + filename, PATH + "/ascii/" + filename);
        }
    }

    /**
     * Méthode utilitaire pour copier un flux de ressource vers un fichier
     * Proposé par Gemini
     */
    private void copyResourceToDisk(String resourcePath, String destinationPath) {
        File targetFile = new File(destinationPath);

        // On ne copie que si le fichier n'existe pas déjà dehors (pour ne pas écraser les modifs user)
        if (!targetFile.exists()) {
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    System.out.println(TAB + "[!] Resource not found in JAR: " + resourcePath);
                    return;
                }
                
                // Copie du flux vers le fichier
                Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println(TAB + "+ Extracted: " + targetFile.getName());
                
            } catch (IOException e) {
                System.out.println(TAB + "- Error extracting " + resourcePath + ": " + e.getMessage());
            }
        }
    }
    
    private void checkAndInstallDependencies() {
        System.out.println(STAR + "Checking dependencies...");

        // GOLANG
        // Si Go existe, on ne fait rien. S'il n'existe pas, on installe.
        if (isCommandAvailable("go")) {
            System.out.println(TAB + STAR + "Go (Golang) is already installed.");
        } else {
            System.out.println(TAB + "[-] Go not found. Attempting installation...");
            installGo();
        }

        // NUCLEI
        if (isCommandAvailable("nuclei")) {
            System.out.println(TAB + STAR + "Nuclei is already installed.");
        } else {
            if (isCommandAvailable("go")) {
                System.out.println(TAB + "[-] Nuclei not found. Installing via Go...");
                installNuclei();
            } else {
                System.out.println(TAB + "[!] Cannot install Nuclei because Go installation failed or is missing.");
            }
        }

        // Si on a besoin de rajouter un autre programme, bah on refait la même chose. Exemple je veux intégrer Nessus
        /*
        if (isCommandAvailable(Nessus) / une autre logique) {
            System.out.println(TAB + STAR + "Nessus is already installed.");
        } else {
            System.out.println(TAB + "[-] Nessus not found. Attempting installation...");
            installNessus();
        }
        */
    }

    private boolean isCommandAvailable(String command) {
        try {
            // "command --version" ou "command -version" marche pour la plupart des outils...
            // pour go c'set version donc bon une exception sinon on fait --version
            ProcessBuilder pb = new ProcessBuilder(command, command.equals("go") ? "version" : "--version");
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void installGo() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        
        // On cible une version très récente qui supporte Nuclei
        String goVersion = "go1.23.4.linux-amd64.tar.gz"; 
        String downloadUrl = "https://go.dev/dl/" + goVersion;

        try {
            if (os.contains("linux")) {
                System.out.println(TAB + STAR + "Detected Linux. 'apt' version is too old.");
                System.out.println(TAB + STAR + "Downloading official Go binary (" + goVersion + ")...");
                
                // Nettoyage d'une éventuelle vieille installation
                /*
                https://go.dev/doc/install
                Remove any previous Go installation by deleting the /usr/local/go folder (if it exists), 
                then extract the archive you just downloaded into /usr/local, creating a fresh Go tree in /usr/local/go: 
                */
                new ProcessBuilder("sudo", "rm", "-rf", "/usr/local/go").start().waitFor();
                
                // Curl
                ProcessBuilder pbDownload = new ProcessBuilder("curl", "-L", "-O", downloadUrl);
                pbDownload.directory(new File("/tmp")); // tmp car c'est une installation temporaire
                pbDownload.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pbDownload.redirectError(ProcessBuilder.Redirect.INHERIT); // Pour voir la progression
                
                if (pbDownload.start().waitFor() != 0) {
                    System.out.println(TAB + "[!] Download failed. Do you have 'curl' installed?");
                    return;
                }

                System.out.println(TAB + STAR + "Extracting Go to /usr/local/go...");
                
                // Extract
                ProcessBuilder pbExtract = new ProcessBuilder("sudo", "tar", "-C", "/usr/local", "-xzf", "/tmp/" + goVersion);
                pbExtract.redirectError(ProcessBuilder.Redirect.INHERIT);
                if (pbExtract.start().waitFor() != 0) {
                    System.out.println(TAB + "[!] Extraction failed.");
                    return;
                }

                // Si c'est la premier fois, faut créer un lien symbolique sinon le terminal va nous dire "connait pas"
                new ProcessBuilder("sudo", "rm", "/usr/bin/go").start().waitFor();
                
                System.out.println(TAB + STAR + "Linking /usr/bin/go...");
                ProcessBuilder pbLink = new ProcessBuilder("sudo", "ln", "-s", "/usr/local/go/bin/go", "/usr/bin/go");
                if (pbLink.start().waitFor() == 0) {
                    System.out.println(TAB + "[+] Go (Manual) installed successfully!");
                } else {
                    System.out.println(TAB + "[!] Symlink failed. You might need to add /usr/local/go/bin to your PATH manually.");
                }

                // Nettoyage du fichier téléchargé
                new File("/tmp/" + goVersion).delete();

            } else {
                System.out.println(TAB + "[!] Automatic Go installation not supported on this OS. Please install manually from https://go.dev/dl/");
            }
        } catch (Exception e) {
            System.out.println(TAB + "[!] Error installing Go: " + e.getMessage());
        }
    }

    private void installNuclei() {
        try {
            System.out.println(TAB + STAR + "Running: go install -v github.com/projectdiscovery/nuclei/v3/cmd/nuclei@latest");
            System.out.println(TAB + "(This might take a while, please wait...)");

            ProcessBuilder pb = new ProcessBuilder("go", "install", "-v", "github.com/projectdiscovery/nuclei/v3/cmd/nuclei@latest");
            pb.redirectError(ProcessBuilder.Redirect.INHERIT); 
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD); 

            int exitCode = pb.start().waitFor();

            if (exitCode == 0) {
                System.out.println(TAB + "[+] Nuclei installed successfully in Go workspace!");
                
                // Idem pour go, on fait lien symbolique
                String userHome = System.getProperty("user.home");
                String goBinPath = userHome + "/go/bin/nuclei";
                String systemBinPath = "/usr/local/bin/nuclei";
                
                System.out.println(TAB + STAR + "Creating global shortcut (Symlink)...");
                
                ProcessBuilder pbLink = new ProcessBuilder("sudo", "ln", "-sf", goBinPath, systemBinPath);
                pbLink.redirectError(ProcessBuilder.Redirect.DISCARD);
                pbLink.redirectInput(ProcessBuilder.Redirect.INHERIT); // Si sudo demande le mdp (mais normalement non car on a déjà sudo avant)
                
                if (pbLink.start().waitFor() == 0) {
                    System.out.println(TAB + "[+] Success! You can now run 'nuclei' from anywhere.");
                } else {
                    System.out.println(TAB + "[!] Could not create shortcut (Permission denied?).");
                    System.out.println(TAB + TAB + "You might need to run: export PATH=$PATH:$HOME/go/bin");
                }
                
            } else {
                System.out.println(TAB + "[!] Nuclei installation failed (Exit code: " + exitCode + ").");
                System.out.println(TAB + "[!] Your Go version might be too old.");
                System.out.println(TAB + "[!] Please install a recent Go version >= 1.24.2 manually.");
            }

        } catch (Exception e) {
            System.out.println(TAB + "[!] Error installing Nuclei: " + e.getMessage());
        }
    }
}