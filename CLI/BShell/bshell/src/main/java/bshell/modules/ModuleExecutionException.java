package bshell.modules;

/* Cette classe permet de créer des exception custom en héritant du module de base Exception*/
public class ModuleExecutionException extends Exception {
    
    // Voici un exemple de surcharge avec 2 constructeurs 

    // 1re constructeur : quand on veut juste signaler l'erreur
    public ModuleExecutionException(String message) {
        super(message);
    }

    // 2ème constructeur : quand on veut en plus expliqué la cause
    public ModuleExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}