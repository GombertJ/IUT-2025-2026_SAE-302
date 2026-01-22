package bshell.modules;

import java.util.Optional;

public class Option {

    private final String name;
    private final String defaultValue;
    private String value;
    private final String description;
    private final boolean required;

    // Constructeur de l'option
    public Option(String name, String defaultValue, String description, boolean required) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.description = description;
        this.required = required;
        this.value = defaultValue;
    }

    // Getter & Setter
    public Optional<String> getValue() {
        String effectiveValue;

        if (value != null && !value.isEmpty()) {
            effectiveValue = value;       // L'utilisateur a défini quelque chose
        } else {
            effectiveValue = defaultValue; // Sinon, on prend la valeur par défaut
        }

        // ofNullable gère le cas où effectiveValue serait null et donc évite de faire planter le programme
        return Optional.ofNullable(effectiveValue);
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /*
    Méthode : Permettant de savoir si l'option contient une valeur utilisable
    */
    public boolean isSet() {
        if (value != null && !value.isEmpty()) {
            return true;
        }
        
        if (defaultValue != null && !defaultValue.isEmpty()) {
            return true;
        }
        
        return false;
    }

    public boolean isRequired() {
        return required;
    }
}