package bshell.database;

import java.util.List;

/*
Elle définit les méthodes a initié si on hérite d'elle.
*/
public interface DatabaseRepository {
    void connect();
    void insertVulnerability(Vulnerability v);
    void updateVulnerabilityState(int id, String newState);
    List<Vulnerability> findByTargetPrefix(String targetPrefix);
}