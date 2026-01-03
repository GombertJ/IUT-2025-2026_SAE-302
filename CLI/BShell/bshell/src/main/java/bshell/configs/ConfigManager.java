package bshell.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    // On instance les classes
    private static ConfigManager instance;
    private Config config;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Constructeur privé car on fait un singleton
    private ConfigManager() {
        load();
    }

    // Singleton
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    // De cette façons on permet d'accéder à la classe configs
    public Config getConfig() {
        return config;
    }

    private File getConfigFile() {
        Config currentConfig;
        
        if (config != null) {
            currentConfig = config;
        } else {
            currentConfig = new Config();
        }

        return new File(currentConfig.workspacePath, "config.json");
    }

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
    
    public String getBinaryPath(String toolName) {
        return config.binaries.getOrDefault(toolName, toolName);
    }
    
    public void setBinaryPath(String toolName, String path) {
        config.binaries.put(toolName, path);
        save();
    }
}