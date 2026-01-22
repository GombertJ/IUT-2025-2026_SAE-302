package bshell.configs;

import java.util.HashMap;
import java.util.Map;

/*
Classe public, on stocke les information pour le fichier de configs.
*/
public class Config {
    public String version = "0.3";
    public String workspacePath = "/opt/bshell/bshell/"; 
    public String dbPath = "/opt/bshell/bshell/bshell.db"; 
    
    public Map<String, String> binaries = new HashMap<>(); 
}