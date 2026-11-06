package es.vargontoc.agentic.viewer.terminal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AnsiStripperTest {

    @Test
    void stripsNullAndEmpty() {
        assertNull(AnsiStripper.strip(null));
        assertEquals("", AnsiStripper.strip(""));
    }

    @Test
    void preservesPlainText() {
        assertEquals("hola mundo", AnsiStripper.strip("hola mundo"));
    }

    @Test
    void stripsCsiColorCodes() {
        // ESC[31m = red, ESC[0m = reset
        String input = "\u001B[31mERROR\u001B[0m: algo falló";
        assertEquals("ERROR: algo falló", AnsiStripper.strip(input));
    }

    @Test
    void stripsCsiCursorPosition() {
        // ESC[2J = clear screen, ESC[H = cursor home
        String input = "\u001B[2J\u001B[Hmenu>";
        assertEquals("menu>", AnsiStripper.strip(input));
    }

    @Test
    void stripsMultipleSequences() {
        String input = "\u001B[1m\u001B[33mAviso\u001B[0m normal";
        assertEquals("Aviso normal", AnsiStripper.strip(input));
    }

    @Test
    void stripsBareEscape() {
        // ESC + letter is a valid escape sequence (e.g., ESC+m is SGR reset)
        assertEquals("textoás", AnsiStripper.strip("texto\u001Bmás"));
    }

    @Test
    void stripsIsolatedEscape() {
        // ESC followed by non-letter (e.g., a digit) gets the ESC removed
        assertEquals("texto1más", AnsiStripper.strip("texto\u001B" + "1más"));
    }
}
