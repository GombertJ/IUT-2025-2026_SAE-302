package bshell.configs;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public String version = "0.2";
    public String workspacePath = "/opt/bshell/bshell/"; 
    public String dbPath = "/opt/bshell/bshell/bshell.db"; 
    
    public Map<String, String> binaries = new HashMap<>(); 
}