package bshell.configs;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public String version = "0.1";
    public String workspacePath = "/opt/bshell/"; 
    public String dbPath = "/opt/bshell/bshell.db"; 
    
    public Map<String, String> binaries = new HashMap<>(); 
}