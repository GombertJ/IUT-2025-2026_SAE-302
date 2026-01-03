package bshell.database;

import java.util.List;
import java.util.Map;

public interface DatabaseRepository {
    void connect();
    void insertVulnerability(Vulnerability v);
    void insertScan(String scanId, Map<String, Object> payload);
    List<Vulnerability> find(String target);
}