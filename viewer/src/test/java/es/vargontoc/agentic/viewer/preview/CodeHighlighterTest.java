package es.vargontoc.agentic.viewer.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CodeHighlighterTest {

    @Test
    void resaltaClaveYValorDeToml() {
        String html = CodeHighlighter.highlight("name = \"mi-agente\"", CodeHighlighter.TOML);

        assertTrue(html.contains("tok-key"), html);
        assertTrue(html.contains(">name</span>"), html);
        assertTrue(html.contains("tok-string"), html);
    }

    @Test
    void resaltaSeccionComentarioYBooleanoDeToml() {
        String html = CodeHighlighter.highlight("""
            # comentario
            [model]
            active = true
            """, CodeHighlighter.TOML);

        assertTrue(html.contains("tok-comment"), html);
        assertTrue(html.contains("tok-section"), html);
        assertTrue(html.contains("tok-bool"), html);
    }

    @Test
    void resaltaClaveYEscalarDeYaml() {
        String html = CodeHighlighter.highlight("name: docs-readme\nmodel: claude-haiku-5-5",
            CodeHighlighter.YAML);

        assertTrue(html.contains("tok-key"), html);
        assertTrue(html.contains(">name</span>"), html);
        assertTrue(html.contains(">docs-readme</span>"), html);
    }

    @Test
    void resaltaBooleanosYListasDeYaml() {
        String html = CodeHighlighter.highlight("omitClaudeMd: true\nskills:\n  - sdd-design",
            CodeHighlighter.YAML);

        assertTrue(html.contains("tok-bool"), html);
        assertTrue(html.contains("tok-punct"), html);
        assertTrue(html.contains(">sdd-design</span>"), html);
    }

    @Test
    void escapaCaracteresEspeciales() {
        String html = CodeHighlighter.highlight("value = \"<hola>\"", CodeHighlighter.TOML);

        assertTrue(html.contains("&lt;hola&gt;"), html);
        assertFalse(html.contains("<hola>"), html);
    }

    @Test
    void lenguajeDesconocidoSoloEscapaSinSpans() {
        String html = CodeHighlighter.highlight("clave = valor", "otro");

        assertFalse(html.contains("tok-"), html);
        assertTrue(html.contains("clave = valor"), html);
    }

    @Test
    void elDocumentoEnvuelveConEtiquetaPre() {
        String document = CodeHighlighter.toDocument("a = 1", CodeHighlighter.TOML);

        assertTrue(document.startsWith("<!DOCTYPE html>"), document);
        assertTrue(document.contains("<pre>"), document);
        assertTrue(document.contains("tok-number"), document);
    }

    @Test
    void devuelveVacioParaTextoNulo() {
        assertEquals("", CodeHighlighter.highlight(null, CodeHighlighter.TOML));
        assertEquals("", CodeHighlighter.highlight("", CodeHighlighter.TOML));
    }
}
