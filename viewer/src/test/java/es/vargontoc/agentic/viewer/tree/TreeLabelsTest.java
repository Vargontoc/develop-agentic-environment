package es.vargontoc.agentic.viewer.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.viewer.cli.model.TreeData;

class TreeLabelsTest {

    @Test
    void marcaTecnicoPendiente() {
        assertEquals("documentador (pendiente)",
            TreeLabels.label(node("technician", "documentador", true, null)));
    }

    @Test
    void marcaAgentePersonalizado() {
        assertEquals("sdd-agent [personalizado]",
            TreeLabels.label(node("agent", "sdd-agent", false, "legacy")));
    }

    @Test
    void marcaAgenteDesvinculado() {
        assertEquals("sdd-agent [desvinculado]",
            TreeLabels.label(node("agent", "sdd-agent", false, "detached")));
    }

    @Test
    void dejaElNombreNormalSinEstado() {
        assertEquals("facturacion", TreeLabels.label(node("module", "facturacion", false, null)));
    }

    @Test
    void marcaHookPendiente() {
        assertEquals("sdd-specs-only (pendiente)",
            TreeLabels.label(node("hook", "sdd-specs-only", true, null)));
    }

    @Test
    void marcaHookPersonalizado() {
        assertEquals("sdd-specs-only [personalizado]",
            TreeLabels.label(node("hook", "sdd-specs-only", false, "legacy")));
    }

    private static TreeData.Node node(String type, String name, boolean pending, String status) {
        return new TreeData.Node(type, name, null, null, status, pending, null);
    }
}
