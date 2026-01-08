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
    
    // On initialise ce qui ne change pas, d'où final
    final String STAR = "[\u2736] ";
    final String TAB = "   ";
    private final ConfigManager configManager;
    private final Config config;

    public Setup() {
        System.out.println();
        System.out.println(STAR + "Setting up BShell environment ...");
        
        // On charge la config dès le début
        this.configManager = ConfigManager.getInstance();
        this.config = configManager.getConfig();

        // Ordre des étapes est importantes !
        createWorkingDirectory(config.workspacePath);
        extractDefaultAsciiArt();
        checkAndInstallDependencies();

        System.out.println(STAR + "Setup complete.");
        System.out.println();
    }

    /*
    Méthode : Permet de mettre en place l'environnement de travail.
    */
    private void createWorkingDirectory(String path) {
        File data = new File(path);
        File ascii = new File(data, "ascii");
        File bin =  new File(data, "bin");

        try {
            if (!data.exists()) {
                data.mkdirs();
                System.out.println(STAR + "Working directory created at: " + data.getAbsolutePath());
            }
            if (!ascii.exists()) {
                ascii.mkdir();
                System.out.println(STAR + "ASCII directory created at: " + ascii.getAbsolutePath());
            }
            if (!bin.exists()) {
                bin.mkdir();
                System.out.println(STAR + "Binary directory created at: " + bin.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Error creating working directories: " + e.getMessage());
        }
    }
    
    /*
    Méthode : Permet d'extraire les ressources Ascii dans le bon dossier
    */
    private void extractDefaultAsciiArt() {
        System.out.println(STAR + "Extracting default resources...");

        String[] defaultFiles = {
            "Bowser.txt", 
            "matrix.txt"
        };
        
        File asciiDir = new File(config.workspacePath, "ascii");

        for (String filename : defaultFiles) {
            File destination = new File(asciiDir, filename);
            
            copyResourceToDisk("/ASCII/" + filename, destination.getAbsolutePath());
        }
    }

    /*
    Méthode : Permet d'extraire dans le JAR des ressources.
    */
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
    
    /*
    Méthode : Permet de vérifier l'installation des fichiers binaires, des compilers et programmes.
    */
    private void checkAndInstallDependencies() {

        File binDir = new File(config.workspacePath, "bin");
        File localNucleiBinary = new File(binDir, "nuclei");
        
        System.out.println(STAR + "Checking dependencies in " + binDir.getAbsolutePath() + " ...");

        // Golang PORTABLE 
        File localGoBinary = new File(binDir, "go/bin/go");

        if (localGoBinary.exists() && localGoBinary.canExecute()) {
            System.out.println(TAB + STAR + "Portable Go is ready.");
            configManager.setBinaryPath("go", localGoBinary.getAbsolutePath());
        } else {
            System.out.println(TAB + "[-] Portable Go not found. Installing...");
            installGo(binDir);
        }
        
        // Nuclei PORTABLE 
        if (localNucleiBinary.exists() && localNucleiBinary.canExecute()) {
             System.out.println(TAB + STAR + "Portable Nuclei is ready.");
             configManager.setBinaryPath("nuclei", localNucleiBinary.getAbsolutePath());
        } else {
             System.out.println(TAB + "[-] Portable Nuclei not found. Installing...");
             if (localGoBinary.exists()) {
                 installNuclei(localGoBinary.getAbsolutePath(), binDir);
             } else {
                 System.out.println(TAB + "[!] Cannot install Nuclei: Go installation failed.");
             }
        }
    }

    /*
    Méthode : Permet d'installer go selon la documentation https://go.dev/doc/install
    */
    private void installGo(File binDir) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        final String goVersion = "go1.23.4.linux-amd64.tar.gz"; 
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
                
                // Extract directement dans le bin.
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

    /*
    Méthode : Permet d'installer nuclei grâce à go
    */
    private void installNuclei(String goCommand, File binDir) {
        try {
            System.out.println(TAB + STAR + "Compiling Nuclei from source (using portable Go)...");
            System.out.println(TAB + "(This might take a while, please wait...)");

            File goDir = new File(binDir, "go"); 

            ProcessBuilder pb = new ProcessBuilder(goCommand, "install", "-v", "github.com/projectdiscovery/nuclei/v3/cmd/nuclei@latest");
            
            // On crée une sorte de sandbox, on dit a go où aller chercher ses binaires car sinon il utilise l'installation par défaut
            Map<String, String> env = pb.environment();
            
            // On spécifie les PATH pour notre environnement, pour qu'il trouve les SDK (software development kit)
            env.put("GOROOT", goDir.getAbsolutePath());
            env.put("GOBIN", binDir.getAbsolutePath());
            env.put("PATH", new File(goDir, "bin").getAbsolutePath() + File.pathSeparator + env.get("PATH"));

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
    
    
    /*
    Méthode : Grâce à la récursivité on supprime proprement les fichiers dans le dossier
    */
   // Pour éviter des mauvaises surprises.
    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        
        if (contents != null) {
            for (File f : contents) deleteDirectory(f);
        }
        
        file.delete();
    }
}