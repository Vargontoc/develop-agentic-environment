package es.vargontoc.agentic.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GreetingTest {

    @Test
    void saludaConUnNombre() {
        assertEquals("Hola, mundo. El esqueleto JavaFX funciona.", Greeting.greet("mundo"));
    }

    @Test
    void recortaEspacios() {
        assertEquals("Hola, mundo. El esqueleto JavaFX funciona.", Greeting.greet("  mundo  "));
    }

    @Test
    void rechazaNombreVacio() {
        assertThrows(IllegalArgumentException.class, () -> Greeting.greet("   "));
    }
}
