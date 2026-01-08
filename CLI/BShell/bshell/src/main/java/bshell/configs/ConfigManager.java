package bshell.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static ConfigManager instance;
    private Config config;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Singleton
    public static synchronized ConfigManager getInstance() {
        
        // Si la classe n'a jamais été appelé alors elle est forcement nul, on crée une seul instance.
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private ConfigManager() {
        load();
    }

    // De cette façons on permet d'accéder à la classe configs
    public Config getConfig() {
        return config;
    }

    /*
    Méthode : on récupére le fichier de configs, si il existe pas on le crée sinon on update
    */
    private File getConfigFile() {
        Config currentConfig;
        
        if (config != null) {
            currentConfig = config;
        } else {
            currentConfig = new Config();
        }

        return new File(currentConfig.workspacePath, "config.json");
    }

    /*
    Méthode : on récupére la configs.
    */
    public void load() {
        File configFile = getConfigFile();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, Config.class);
            } catch (IOException e) {
                System.err.println("[!] Erreur chargement config, utilisation défaut.");
                config = new Config();
            }
        } else {
            config = new Config();
            save();
        }
    }

    /*
    Méthode : on sauvegarde la configs
    */
    public void save() {
        File configFile = getConfigFile();

        try {
            File parentDir = configFile.getParentFile();
            
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("[!] Impossible de sauvegarder la config: " + e.getMessage());
        }
    }
    
    /*
    Méthode : on récupére le chemin d'accès d'un outil
    */
    public String getBinaryPath(String toolName) {
        return config.binaries.getOrDefault(toolName, toolName);
    }
    
    /*
    Méthode : on définit le chemin d'accès d'un outil
    */
    public void setBinaryPath(String toolName, String path) {
        config.binaries.put(toolName, path);
        save();
    }
}