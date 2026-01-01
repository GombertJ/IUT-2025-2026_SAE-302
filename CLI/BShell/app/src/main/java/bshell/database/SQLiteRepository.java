package bshell.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQLiteRepository implements DatabaseRepository {

    private final String dbPath;
    private Connection conn;

    public SQLiteRepository(String dbPath) {
        this.dbPath = dbPath;
        // On se connecte dès l'instanciation ou via la méthode connect()
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
            // System.out.println("[DB] Vulnérabilité sauvegardée: " + v.getName());
        } catch (SQLException e) {
            System.err.println("[DB] Erreur insert: " + e.getMessage());
        }
    }

    @Override
    public List<Vulnerability> find(String target) {
        List<Vulnerability> list = new ArrayList<>();
        // Recherche exacte ou partielle sur la cible
        String sql = "SELECT id, name, target, state, infos FROM cves WHERE target LIKE ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + target + "%"); // Recherche "contient"
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Vulnerability(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("target"),
                        rs.getString("state"),
                        rs.getString("infos")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erreur find: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void insertScan(String scanId, Map<String, Object> payload) {
        // Implémentation future ou placeholder pour respecter le diagramme
        System.out.println("[DB] TODO: Implement insertScan");
    }
}