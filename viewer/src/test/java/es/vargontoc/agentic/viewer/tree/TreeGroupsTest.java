package es.vargontoc.agentic.viewer.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.viewer.cli.model.TreeData;

class TreeGroupsTest {

    @Test
    void agrupaLosArtefactosDeUnProveedorPorTipo() {
        TreeData.Node provider = new TreeData.Node(
            "provider", "claude", "C:/proyecto/.claude", "claude", null, false,
            List.of(
                node("agent", "analista"),
                node("agent", "desarrollador"),
                node("skill", "team-conventions"),
                node("profile", "permisos")));

        List<TreeData.Node> grouped = TreeGroups.group(List.of(provider));

        TreeData.Node result = grouped.getFirst();
        assertEquals(3, result.children().size());
        assertEquals("Agentes", result.children().get(0).name());
        assertEquals(2, result.children().get(0).children().size());
        assertEquals("Skills", result.children().get(1).name());
        assertEquals("Perfiles", result.children().get(2).name());
        assertEquals("claude", result.children().get(0).provider());
    }

    @Test
    void noAgrupaSiSoloHayUnArtefacto() {
        TreeData.Node provider = node("provider", "claude", node("agent", "analista"));

        List<TreeData.Node> grouped = TreeGroups.group(List.of(provider));

        assertEquals("agent", grouped.getFirst().children().getFirst().type());
    }

    @Test
    void agrupaTambienBajoUnTecnico() {
        TreeData.Node module = node("module", "facturacion",
            node("technician", "analista",
                node("agent", "analista-facturacion"),
                node("skill", "team-conventions")));

        List<TreeData.Node> grouped = TreeGroups.group(List.of(module));

        TreeData.Node technician = grouped.getFirst().children().getFirst();
        assertEquals("group", technician.children().getFirst().type());
        assertEquals("Agentes", technician.children().getFirst().name());
        assertEquals("Skills", technician.children().get(1).name());
    }

    @Test
    void conservaHijosQueNoSonArtefactos() {
        TreeData.Node provider = node("provider", "claude",
            node("agent", "a"), node("agent", "b"), node("sdd", ".sdd"));

        List<TreeData.Node> grouped = TreeGroups.group(List.of(provider));

        assertTrue(grouped.getFirst().children().stream().anyMatch(child -> "sdd".equals(child.type())));
    }

    private static TreeData.Node node(String type, String name, TreeData.Node... children) {
        return new TreeData.Node(type, name, "C:/proyecto/" + name, null, null, false, List.of(children));
    }
}
