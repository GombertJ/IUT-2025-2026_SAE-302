package bshell.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        String sql = "CREATE TABLE IF NOT EXISTS cve (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "name TEXT NOT NULL, " +
                     "target TEXT NOT NULL, " +
                     "state TEXT NOT NULL, " +
                     "infos TEXT NOT NULL" +
                     ");";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("[DB] Erreur création tables: " + e.getMessage());
        }
    }

    @Override
    public void insertVulnerability(Vulnerability v) {
        String sql = "INSERT INTO cve(name, target, state, infos) VALUES(?, ?, ?, ?)";

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

    @Override
    public void updateVulnerabilityState(int id, String newState) {
        String sql = "UPDATE cve SET state = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newState);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Erreur update state: " + e.getMessage());
        }
    }

    @Override
    public List<Vulnerability> findByTargetPrefix(String targetPrefix) {
        List<Vulnerability> results = new ArrayList<>();
        String sql = "SELECT id, name, target, state, infos FROM cve WHERE target LIKE ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, targetPrefix + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Vulnerability(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("target"),
                        rs.getString("state"),
                        rs.getString("infos")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erreur select: " + e.getMessage());
        }
        return results;
    }
}