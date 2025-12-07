package bshell.modules;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class ModuleManager {

    // Thread-safe map pour stocker les modules
    private final ConcurrentMap<String, Module> modules = new ConcurrentHashMap<>();

    public ModuleManager() {
        // Enregistrement automatique des modules disponibles au démarrage
        try {
            register(new NmapModule());
            // register(new ExampleExploit()); // Plus tard
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

    // Recherche simple (ex: search "nm" trouve "nmap") [cite: 168]
    public List<Module> search(String prefix) {
        return modules.values().stream()
                .filter(m -> m.getName().toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}