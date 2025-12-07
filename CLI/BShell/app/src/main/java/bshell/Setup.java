package bshell;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Setup {
    
    // Variables 
    String PATH = "data";
    String star = "[\u2736] ";

    public Setup() {
        System.out.println();
        System.out.println(star + "Setting up BShell environment...");

        // Ordre des étapes est importantes ! (Ne pas inverser)
        createWorkingDirectory(PATH);
        createConfigFile();
        extractDefaultAsciiArt();

        System.out.println(star + "Setup complete.");
        System.out.println();
    }

    private void createWorkingDirectory(String path) {
        File data = new File(path);
        File ascii = new File(path + "/ascii");
        File bin =  new File(path + "/bin");

        try {
            if (!data.exists()) {
                data.mkdirs();
                System.out.println(star + "Working directory created at: " + path);
            }
            if (!ascii.exists()) {
                ascii.mkdir();
                System.out.println(star + "ASCII directory created at: " + path + "/ascii");
            }
            if (!bin.exists()) {
                bin.mkdir();
                System.out.println(star + "Binary directory created at: " + path + "/bin");
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
                System.out.println(star + "Configuration file created at: " + PATH + "/config.json");
            }
        } catch (Exception e) {
            System.out.println("Error creating configuration file: " + e.getMessage());
        }
    }

    /**
     * Copie les fichiers ressources internes vers le dossier externe data/ascii
     */
    private void extractDefaultAsciiArt() {
        System.out.println(star + "Extracting default resources...");

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
                    System.out.println("   [!] Resource not found in JAR: " + resourcePath);
                    return;
                }
                
                // Copie du flux vers le fichier
                Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("   + Extracted: " + targetFile.getName());
                
            } catch (IOException e) {
                System.out.println("   - Error extracting " + resourcePath + ": " + e.getMessage());
            }
        }
    }
}