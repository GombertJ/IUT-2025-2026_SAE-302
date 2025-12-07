package bshell.modules;

public class ModuleExecutionException extends Exception {
    
    public ModuleExecutionException(String message) {
        super(message);
    }

    public ModuleExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}