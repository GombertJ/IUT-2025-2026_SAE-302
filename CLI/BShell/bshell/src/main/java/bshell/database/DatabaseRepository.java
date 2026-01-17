package bshell.database;

import java.util.List;

public interface DatabaseRepository {
    void connect();
    void insertVulnerability(Vulnerability v);
    void updateVulnerabilityState(int id, String newState);
    List<Vulnerability> findByTargetPrefix(String targetPrefix);
}