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
    
    private final File configFile = new File("data/config.json");
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {
        load();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public Config getConfig() {
        return config;
    }

    public void load() {
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
        try {
            // CRUCIAL : On s'assure que le dossier parent (data/) existe avant d'écrire
            if (configFile.getParentFile() != null && !configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
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