package bshell.modules;

import java.util.Optional;

public class Option {

    private final String name;
    private final String defaultValue;
    private String value;
    private final String description;
    private final boolean required;

    // Correspond au constructeur UML [cite: 199]
    public Option(String name, String defaultValue, String description, boolean required) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.description = description;
        this.required = required;
        this.value = defaultValue; // Par défaut, la valeur est la valeur par défaut
    }

    public String getName() {
        return name;
    }

    public Optional<String> getValue() {
        return Optional.ofNullable(value != null && !value.isEmpty() ? value : defaultValue);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isSet() {
        // Une option est "set" si elle a une valeur ou une valeur par défaut
        return (value != null && !value.isEmpty()) || (defaultValue != null && !defaultValue.isEmpty());
    }
}