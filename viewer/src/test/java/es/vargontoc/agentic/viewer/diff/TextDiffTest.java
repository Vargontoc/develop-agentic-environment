package es.vargontoc.agentic.viewer.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextDiffTest {

    @Test
    void sinCambiosTodasLasLineasSonContexto() {
        assertEquals(" uno\n dos\n", TextDiff.between("uno\ndos", "uno\ndos"));
    }

    @Test
    void marcaLineaEliminadaYAnadida() {
        assertEquals("-viejo\n+nuevo\n", TextDiff.between("viejo", "nuevo"));
    }

    @Test
    void conservaElContextoAlCambiarUnaLinea() {
        String diff = TextDiff.between("a\nb\nc", "a\nB\nc");

        assertEquals(" a\n-b\n+B\n c\n", diff);
    }

    @Test
    void detectaLineasAnadidasYQuitadas() {
        assertEquals(" a\n+b\n", TextDiff.between("a", "a\nb"));
        assertEquals(" a\n-b\n", TextDiff.between("a\nb", "a"));
    }

    @Test
    void unTextoVacioFrenteAOtroMuestraSoloAnadidos() {
        assertEquals("+a\n+b\n", TextDiff.between("", "a\nb"));
        assertEquals("", TextDiff.between("", ""));
    }

    @Test
    void elSaltoFinalNoGeneraUnaLineaVaciaExtra() {
        String diff = TextDiff.between("a\n", "a\n");

        assertEquals(" a\n", diff);
        assertTrue(diff.endsWith("\n"));
    }

    @Test
    void calculaUnDiffUtilizableConMuchasLineas() {
        StringBuilder oldText = new StringBuilder();
        StringBuilder newText = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            oldText.append("linea ").append(i).append('\n');
            newText.append("linea ").append(i).append(i == 250 ? " cambiada" : "").append('\n');
        }

        String diff = TextDiff.between(oldText.toString(), newText.toString());

        assertTrue(diff.contains("-linea 250\n"));
        assertTrue(diff.contains("+linea 250 cambiada\n"));
        assertTrue(diff.contains(" linea 0\n"));
    }
}
