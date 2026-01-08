package bshell.database;

import java.sql.*;

public class SQLiteRepository implements DatabaseRepository {

    private final String dbPath;
    private Connection conn;

    public SQLiteRepository(String dbPath) {
        this.dbPath = dbPath;
        connect(); 
        initTables();
    }

    @Override
    public void connect() {
        try {
            if (conn != null && !conn.isClosed()) return;

            // URL JDBC pour SQLite
            String url = "jdbc:sqlite:" + dbPath;
            conn = DriverManager.getConnection(url);
            System.out.println("[DB] Connecté à la base SQLite: " + dbPath);

        } catch (SQLException e) {
            System.err.println("[DB] Erreur de connexion: " + e.getMessage());
        }
    }

    private void initTables() {
        // Création de la table si elle n'existe pas
        String sql = "CREATE TABLE IF NOT EXISTS cves (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "name TEXT NOT NULL, " +
                     "target TEXT, " +
                     "state TEXT, " +
                     "infos TEXT" +
                     ");";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("[DB] Erreur création tables: " + e.getMessage());
        }
    }

    @Override
    public void insertVulnerability(Vulnerability v) {
        String sql = "INSERT INTO cves(name, target, state, infos) VALUES(?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, v.getName());
            pstmt.setString(2, v.getTarget());
            pstmt.setString(3, v.getState());
            pstmt.setString(4, v.getInfos());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Erreur insert: " + e.getMessage());
        }
    }
}