package bshell.modules;

import java.io.PrintStream;
import java.util.Map;
import java.util.Optional;

public interface Module {

    String getName();

    String getDescription();

    Map<String, Option> getOptions();

    void setOption(String key, String value) throws IllegalArgumentException;

    Optional<String> getOption(String key);

    void run() throws ModuleExecutionException;

    String info();

    void showOptions(PrintStream out);
}