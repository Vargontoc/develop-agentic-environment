package es.vargontoc.agentic.viewer.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SharedBodyTest {

    @Test
    void detectaElPlaceholder() {
        assertTrue(SharedBody.hasPlaceholder("Eres analista-{module}"));
        assertFalse(SharedBody.hasPlaceholder("Eres analista-facturacion"));
        assertFalse(SharedBody.hasPlaceholder(null));
    }

    @Test
    void resuelveElPlaceholder() {
        assertEquals("Eres analista-facturacion",
            SharedBody.resolve("Eres analista-{module}", "facturacion"));
        assertEquals("Eres analista-{module}", SharedBody.resolve("Eres analista-{module}", null));
        assertEquals("", SharedBody.resolve(null, "facturacion"));
    }

    @Test
    void resaltaElPlaceholderEnElHtml() {
        String html = SharedBody.toHtml("Eres {module} del proyecto", "facturacion", false);

        assertTrue(html.contains("class=\"placeholder\""), html);
        assertTrue(html.contains(">{module}</span>"), html);
        assertTrue(html.contains("Eres "), html);
    }

    @Test
    void laVersionResueltaSustituyeElPlaceholder() {
        String html = SharedBody.toHtml("Eres {module}", "facturacion", true);

        assertFalse(html.contains("placeholder"), html);
        assertTrue(html.contains("facturacion"), html);
    }

    @Test
    void escapaCaracteresEspeciales() {
        String html = SharedBody.toHtml("Eres <{module}> & más", "m", false);

        assertTrue(html.contains("&lt;"), html);
        assertTrue(html.contains("&amp;"), html);
        assertFalse(html.contains("<{module}>"), html);
    }

    @Test
    void elDocumentoEnvuelveConEstilos() {
        String document = SharedBody.toDocument("Eres {module}", "facturacion", false);

        assertTrue(document.startsWith("<!DOCTYPE html>"), document);
        assertTrue(document.contains("<pre>"), document);
        assertTrue(document.contains("placeholder"), document);
    }
}
