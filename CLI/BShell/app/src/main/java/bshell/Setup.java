package bshell;

import bshell.configs.Config;
import bshell.configs.ConfigManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;

public class Setup {
    
    String STAR = "[\u2736] ";
    String TAB = "   ";
    
    private final ConfigManager configManager;
    private final Config config;

    public Setup() {
        System.out.println();
        System.out.println(STAR + "Setting up BShell environment (Portable Mode)...");
        
        // On charge la config dès le début
        this.configManager = ConfigManager.getInstance();
        this.config = configManager.getConfig();

        // Ordre des étapes est importantes !
        createWorkingDirectory(config.workspacePath);
        
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

    private void extractDefaultAsciiArt() {
        System.out.println(STAR + "Extracting default resources...");

        String[] defaultFiles = {
            "Bowser.txt", 
            "maskass.txt", 
            "rammus.txt",
            "creeper.txt"
        };

        for (String filename : defaultFiles) {
            copyResourceToDisk("/ASCII/" + filename, config.workspacePath + "/ascii/" + filename);
        }
    }

    private void copyResourceToDisk(String resourcePath, String destinationPath) {
        File targetFile = new File(destinationPath);

        if (!targetFile.exists()) {
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    System.out.println(TAB + "[!] Resource not found in JAR: " + resourcePath);
                    return;
                }
                Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println(TAB + "+ Extracted: " + targetFile.getName());
            } catch (IOException e) {
                System.out.println(TAB + "- Error extracting " + resourcePath + ": " + e.getMessage());
            }
        }
    }
    
    private void checkAndInstallDependencies() {
        System.out.println(STAR + "Checking dependencies in " + config.workspacePath + "/bin ...");

        File binDir = new File(config.workspacePath + "/bin");

        // OLANG PORTABLE 
        File localGoBinary = new File(binDir, "go/bin/go");

        if (localGoBinary.exists() && localGoBinary.canExecute()) {
            System.out.println(TAB + STAR + "Portable Go is ready.");
            // On s'assure que la config pointe bien dessus
            configManager.setBinaryPath("go", localGoBinary.getAbsolutePath());
        } else {
            System.out.println(TAB + "[-] Portable Go not found. Installing...");
            installGo(binDir);
        }

        // NUCLEI PORTABLE 
        File localNucleiBinary = new File(binDir, "nuclei");

        if (localNucleiBinary.exists() && localNucleiBinary.canExecute()) {
            System.out.println(TAB + STAR + "Portable Nuclei is ready.");
            // On s'assure que la config pointe bien dessus
            configManager.setBinaryPath("nuclei", localNucleiBinary.getAbsolutePath());
        } else {
            System.out.println(TAB + "[-] Portable Nuclei not found. Installing...");
            
            // On a besoin de Go pour installer Nuclei
            // On est sûr qu'il existe car on vient de le traiter juste au-dessus
            if (localGoBinary.exists()) {
                installNuclei(localGoBinary.getAbsolutePath(), binDir);
            } else {
                System.out.println(TAB + "[!] Cannot install Nuclei: Go installation failed.");
            }
        }
    }

    private void installGo(File binDir) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        String goVersion = "go1.23.4.linux-amd64.tar.gz"; 
        String downloadUrl = "https://go.dev/dl/" + goVersion;
        
        try {
            if (os.contains("linux")) {
                System.out.println(TAB + STAR + "Downloading Go binary...");
                
                // Nettoyage préventif
                File goInstallDir = new File(binDir, "go");
                if (goInstallDir.exists()) {
                    deleteDirectory(goInstallDir);
                }
                
                // Curl vers /tmp
                ProcessBuilder pbDownload = new ProcessBuilder("curl", "-L", "-O", downloadUrl);
                pbDownload.directory(new File("/tmp"));
                pbDownload.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                
                if (pbDownload.start().waitFor() != 0) {
                    System.out.println(TAB + "[!] Download failed.");
                    return;
                }

                System.out.println(TAB + STAR + "Extracting Go...");
                
                // Extract directement dans data/bin
                ProcessBuilder pbExtract = new ProcessBuilder("tar", "-C", binDir.getAbsolutePath(), "-xzf", "/tmp/" + goVersion);
                if (pbExtract.start().waitFor() != 0) {
                    System.out.println(TAB + "[!] Extraction failed.");
                    return;
                }

                // Vérification finale
                File goBinary = new File(goInstallDir, "bin/go");
                if (goBinary.exists()) {
                    System.out.println(TAB + "[+] Go installed successfully.");
                    configManager.setBinaryPath("go", goBinary.getAbsolutePath());
                } else {
                     System.out.println(TAB + "[!] Installation failed: binary not found.");
                }

                // Nettoyage tmp
                new File("/tmp/" + goVersion).delete();

            } else {
                System.out.println(TAB + "[!] Automatic Go installation not supported on this OS.");
            }
        } catch (Exception e) {
            System.out.println(TAB + "[!] Error installing Go: " + e.getMessage());
        }
    }

    private void installNuclei(String goCommand, File binDir) {
        try {
            System.out.println(TAB + STAR + "Compiling Nuclei from source (using portable Go)...");
            System.out.println(TAB + "(This might take a while, please wait...)");

            File goDir = new File(binDir, "go"); 

            ProcessBuilder pb = new ProcessBuilder(goCommand, "install", "-v", "github.com/projectdiscovery/nuclei/v3/cmd/nuclei@latest");
            
            // On crée une sorte de sandbox, on dit a go où aller chercher ses binaires car sinon il utilise l'installation par défaut
            Map<String, String> env = pb.environment();
            
            env.put("GOROOT", goDir.getAbsolutePath());
            env.put("GOBIN", binDir.getAbsolutePath());
            env.put("PATH", new File(goDir, "bin").getAbsolutePath() + File.pathSeparator + env.get("PATH"));
            // ------------------------------------

            pb.redirectError(ProcessBuilder.Redirect.INHERIT); 
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD); 

            int exitCode = pb.start().waitFor();

            if (exitCode == 0) {
                File nucleiFile = new File(binDir, "nuclei");
                if (nucleiFile.exists()) {
                     System.out.println(TAB + "[+] Nuclei installed successfully.");
                     configManager.setBinaryPath("nuclei", nucleiFile.getAbsolutePath());
                } else {
                    System.out.println(TAB + "[!] Compilation success but binary missing.");
                }
            } else {
                System.out.println(TAB + "[!] Nuclei installation failed (Exit code: " + exitCode + ").");
            }

        } catch (Exception e) {
            System.out.println(TAB + "[!] Error installing Nuclei: " + e.getMessage());
        }
    }
    
    // Pour éviter des mauvaises surprises.
    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        
        if (contents != null) {
            for (File f : contents) deleteDirectory(f);
        }
        
        file.delete();
    }
}