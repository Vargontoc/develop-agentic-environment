package es.vargontoc.agentic.viewer.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.viewer.cli.model.TreeData;

class TreePresentationTest {

    @Test
    void elTooltipIncluyeTipoEstadoRutaYCambios() {
        TreeData.Node node = new TreeData.Node(
            "agent", "analista", "C:/proyecto/agente.md", "claude", "legacy", false, null);

        String tooltip = TreePresentation.tooltip(node, true, true);

        assertTrue(tooltip.contains("Agente"));
        assertTrue(tooltip.contains("personalizado"));
        assertTrue(tooltip.contains("C:/proyecto/agente.md"));
        assertTrue(tooltip.contains("Cambios sin guardar"));
        assertTrue(tooltip.contains("Desactualizado"));
    }

    @Test
    void etiquetaLosTiposConocidos() {
        assertTrue("Hook".equals(TreePresentation.kindLabel("hook")));
        assertTrue("Perfil".equals(TreePresentation.kindLabel("profile")));
    }
}
