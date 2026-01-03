package bshell.modules;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractModule implements Module {

    private final String name;
    private final String description;
    // LinkedHashMap garde l'ordre d'insertion (plus propre pour l'affichage)
    /*
    Pk une LinkedHashMap au lieu d'une HashMap ?

    Explication : Disons que je suis sur python, j'aimerai avoir une sorte de dictionnaire (clé -> valeur) mais ordonnée.
    Et bah sur Java, il y a une structure répondant à ses critères.

    -> Linked = Ordonnée
    -> HashMap = N'est pas ordonnée
    */ 
    protected final Map<String, Option> options = new LinkedHashMap<>();

    public AbstractModule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected void registerOption(String name, String defaultValue, String description, boolean required) {
        this.options.put(name.toUpperCase(), new Option(name.toUpperCase(), defaultValue, description, required));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, Option> getOptions() {
        return options;
    }

    @Override
    public void setOption(String key, String value) throws IllegalArgumentException {
        Option opt = options.get(key.toUpperCase());
        if (opt == null) {
            throw new IllegalArgumentException("Option inconnue : " + key);
        }
        opt.setValue(value);
    }

    @Override
    public Optional<String> getOption(String key) {
        Option opt = options.get(key.toUpperCase());
        if (opt != null) {
            return opt.getValue();
        }
        return Optional.empty();
    }

    public void showOptions(PrintStream out) {
        out.println("\nOptions du module (" + name + ") :");
        out.println("=========================================");
        // Formatage simple en colonnes
        String format = "%-15s %-10s %-15s %-30s%n";
        out.printf(format, "Nom", "Requis", "Valeur", "Description");
        out.printf(format, "---", "------", "------", "-----------");

        for (Option opt : options.values()) {
            String val = opt.getValue().orElse("");
            out.printf(format, 
                opt.getName(), 
                opt.isRequired() ? "yes" : "no", 
                val, 
                opt.getDescription()
            );
        }
        out.println();
    }

    @Override
    public String info() {
        return "Module: " + name + "\nDescription: " + description;
    }
}