package bshell.database;

public interface DatabaseRepository {
    void connect();
    void insertVulnerability(Vulnerability v);
}