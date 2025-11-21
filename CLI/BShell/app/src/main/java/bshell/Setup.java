package bshell;

import java.io.File;

public class Setup {

    String PATH = "data";
    String star = "[\u2736] ";

    public Setup() {

        System.out.println();

        System.out.println(star + "Setting up BShell environment...");

        createWorkingDirectory(PATH);
        createConfigFile();
        injectAsciiArt();

        System.out.println(star + "Setup complete.");
        System.out.println();
    }

    private void createWorkingDirectory(String path) {
        File data = new File(path);
        File ascii = new File(path + "/ascii");
        File bin =  new File(path + "/bin");

        try {
            if (!data.exists()){
            data.mkdir();
            System.out.println(star + "Working directory created at: " + path);
            }

            if (!ascii.exists()){
                ascii.mkdir();
                System.out.println(star + "ASCII directory created at: " + path + "/ascii");
            }

            if (!bin.exists()){
                bin.mkdir();
                System.out.println(star + "Binary directory created at: " + path + "/bin");
            }
        } catch (Exception e) {
            System.out.println("Error creating working directories: " + e.getMessage());  }
        
        
    }

    private void createConfigFile() {
        // json file
        System.out.println(star + "Creating configuration file...");
        File config = new File(PATH + "/config.json");
        try {
            if (config.createNewFile()) {
                System.out.println(star + "Configuration file created at: " + PATH + "/config.json");
            } else {
                System.out.println(star + "Configuration file already exists at: " + PATH + "/config.json");
            }
        } catch (Exception e) {
            System.out.println("Error creating configuration file: " + e.getMessage());
        }

    }

    private void injectAsciiArt() {

    }


}
