package bshell.modules;

import java.io.PrintStream;
import java.util.Map;
import java.util.Optional;

public interface Module {

    String getName();
    String getDescription();
    String info();
    
    Map<String, Option> getOptions();
    Optional<String> getOption(String key);

    void run() throws ModuleExecutionException;
    void setOption(String key, String value) throws IllegalArgumentException;
    void showOptions(PrintStream out);
}