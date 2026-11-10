package es.vargontoc.agentic.viewer.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DiffHighlighterTest {

    @Test
    void clasificaLasLineasDelDiff() {
        assertEquals(DiffHighlighter.ADDED, DiffHighlighter.styleClass("+ nueva"));
        assertEquals(DiffHighlighter.REMOVED, DiffHighlighter.styleClass("- vieja"));
        assertEquals(DiffHighlighter.HUNK, DiffHighlighter.styleClass("@@ -1 +1 @@"));
        assertEquals(DiffHighlighter.HEADER, DiffHighlighter.styleClass("+++ b/fichero"));
        assertEquals(DiffHighlighter.HEADER, DiffHighlighter.styleClass("--- a/fichero"));
        assertEquals(DiffHighlighter.HEADER, DiffHighlighter.styleClass("diff --git a/x b/x"));
        assertEquals(DiffHighlighter.TITLE, DiffHighlighter.styleClass("Miembros: 2 · actualizará: 2"));
        assertEquals(DiffHighlighter.LINE, DiffHighlighter.styleClass(" contexto"));
        assertEquals(DiffHighlighter.LINE, DiffHighlighter.styleClass(""));
    }
}
