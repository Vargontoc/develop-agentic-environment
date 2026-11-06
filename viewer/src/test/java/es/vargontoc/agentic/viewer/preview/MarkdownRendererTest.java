package es.vargontoc.agentic.viewer.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void convierteEncabezadosListasYCodigo() {
        String html = renderer.toHtml("# Título\n\n- uno\n- dos\n\n`código`");

        assertTrue(html.contains("<h1>Título</h1>"), html);
        assertTrue(html.contains("<li>uno</li>"), html);
        assertTrue(html.contains("<code>código</code>"), html);
    }

    @Test
    void escapaHtmlIncrustado() {
        String html = renderer.toHtml("<script>alert(1)</script>");

        assertTrue(html.contains("&lt;script&gt;"), html);
    }

    @Test
    void devuelveVacioParaTextoNuloOVacio() {
        assertEquals("", renderer.toHtml(null));
        assertEquals("", renderer.toHtml(""));
        assertEquals("", renderer.toHtml("   "));
    }

    @Test
    void elDocumentoEnvuelveElHtmlConEstilos() {
        String document = renderer.toDocument("**negrita**");

        assertTrue(document.startsWith("<!DOCTYPE html>"), document);
        assertTrue(document.contains("<strong>negrita</strong>"), document);
        assertTrue(document.contains("<style>"), document);
    }
}
