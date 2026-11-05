package es.vargontoc.agentic.viewer.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.viewer.cli.model.TreeData;

class TreeModelTest {

    @Test
    void creaLasDosRaices() {
        TreeData data = new TreeData(List.of(), List.of(), List.of(), List.of());

        List<TreeData.Node> roots = TreeModel.roots(data);

        assertEquals(2, roots.size());
        assertEquals("Global", roots.get(0).name());
        assertEquals("Project", roots.get(1).name());
    }

    @Test
    void conservaProveedoresModulosYTecnicosPendientes() {
        TreeData data = fixture();
        TreeData.Node project = TreeModel.roots(data).get(1);

        TreeData.Node module = project.children().stream()
            .filter(node -> "module".equals(node.type()))
            .findFirst()
            .orElseThrow();
        TreeData.Node pending = module.children().stream()
            .filter(node -> "technician".equals(node.type()) && node.pending())
            .findFirst()
            .orElseThrow();

        assertEquals("facturacion", module.name());
        assertEquals("documentador", pending.name());
    }

    @Test
    void elRouterApareceUnaSolaVezBajoElProveedor() {
        TreeData.Node project = TreeModel.roots(fixture()).get(1);
        TreeData.Node provider = project.children().stream()
            .filter(node -> "provider".equals(node.type()))
            .findFirst()
            .orElseThrow();

        long routers = provider.children().stream()
            .filter(node -> "analista-router".equals(node.name()))
            .count();

        assertEquals(1, routers);
    }

    @Test
    void noHayProyectoDevuelveRamaVacia() {
        TreeData data = new TreeData(List.of(), null, List.of(), List.of());

        TreeData.Node project = TreeModel.roots(data).get(1);

        assertTrue(project.children().isEmpty());
    }

    private static TreeData fixture() {
        TreeData.Node provider = node("provider", "claude", null, null, false, List.of(
            node("agent", "analista-router", "C:/proyecto/.claude/agents/analista-router.md", "linked", false, List.of())));

        TreeData.Node analyser = node("technician", "analista", null, null, false, List.of(
            node("agent", "analista-facturacion", "C:/proyecto/.claude/agents/analista-facturacion.md", "linked", false, List.of())));
        TreeData.Node documenter = node("technician", "documentador", null, null, true, List.of());
        TreeData.Node module = node("module", "facturacion", null, null, false, List.of(analyser, documenter));

        return new TreeData(List.of(provider), List.of(module, provider), List.of(), List.of());
    }

    private static TreeData.Node node(String type, String name, String path, String status,
                                      boolean pending, List<TreeData.Node> children) {
        return new TreeData.Node(type, name, path, null, status, pending, children);
    }
}
