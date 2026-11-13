package es.vargontoc.agentic.viewer.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CreateArtifactWizardTest {

    @Test
    void laClaveIdentificaProveedorAmbitoYTipo() {
        CreateArtifactWizard.CatalogItem item = item("claude", "global", "agent", "sdd-design");

        assertEquals("claude|global|agent|sdd-design", item.key());
        assertEquals("Agente", item.kindLabel());
    }

    @Test
    void laPeticionCopiaLasListasYElContenido() {
        Map<String, String> contents = new HashMap<>();
        contents.put("k", "texto");
        CreateArtifactWizard.Request request = new CreateArtifactWizard.Request(
            "global", List.of(item("claude", "global", "skill", "docs")), contents, false);

        contents.put("otro", "x");

        assertEquals(1, request.items().size());
        assertEquals("texto", request.contents().get("k"));
        assertTrue(!request.contents().containsKey("otro"));
    }

    @Test
    void elContenidoNuloSeConvierteEnMapaVacio() {
        CreateArtifactWizard.Request request = new CreateArtifactWizard.Request(
            "global", List.of(), null, false);

        assertTrue(request.contents().isEmpty());
    }

    private static CreateArtifactWizard.CatalogItem item(String provider, String scope, String kind, String name) {
        return new CreateArtifactWizard.CatalogItem(provider, scope, kind, name, name, null, null, false);
    }
}
