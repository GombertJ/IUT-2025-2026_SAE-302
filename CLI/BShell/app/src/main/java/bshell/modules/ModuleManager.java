package bshell.modules;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ModuleManager {

    // Thread-safe map pour stocker les modules
    private final ConcurrentMap<String, Module> modules = new ConcurrentHashMap<>();

    public ModuleManager() {
        // Enregistrement automatique des modules disponibles au démarrage
        try {
            register(new NucleiModule());
        } catch (IllegalArgumentException e) {
            System.err.println("Erreur au chargement des modules: " + e.getMessage());
        }
    }

    public void register(Module m) {
        if (modules.containsKey(m.getName().toLowerCase())) {
            throw new IllegalArgumentException("Module déjà enregistré : " + m.getName());
        }
        modules.put(m.getName().toLowerCase(), m);
    }

    public Optional<Module> get(String name) {
        return Optional.ofNullable(modules.get(name.toLowerCase()));
    }

    public Collection<Module> list() {
        return modules.values();
    }
}