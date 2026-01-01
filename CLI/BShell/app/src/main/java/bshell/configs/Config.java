package bshell.configs;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public String version = "0.1";
    public String workspacePath = "data"; 
    public String dbPath = "data/dev.db"; 
    
    public Map<String, String> binaries = new HashMap<>(); 
}