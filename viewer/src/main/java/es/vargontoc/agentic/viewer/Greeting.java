package es.vargontoc.agentic.viewer;

/**
 * Lógica mínima de TASK-001 para comprobar que la ventana y la interacción
 * funcionan sin depender todavía del CLI.
 */
public final class Greeting {

    private Greeting() {}

    public static String greet(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        return "Hola, " + clean + ". El esqueleto JavaFX funciona.";
    }
}
