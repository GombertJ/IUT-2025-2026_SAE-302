package bshell.database;

import bshell.configs.ConfigManager;

public class DatabaseService {

    private static DatabaseService instance;
    private final DatabaseRepository repo;

    private DatabaseService() {
        String dbPath = ConfigManager.getInstance().getConfig().dbPath;
        
        this.repo = new SQLiteRepository(dbPath);
    }

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }
    
    public void saveVulnerability(String name, String target, String state, String infos) {
        if (repo == null) return;
        repo.insertVulnerability(new Vulnerability(name, target, state, infos));
    }
}