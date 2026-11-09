package es.vargontoc.agentic.viewer.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.viewer.cli.model.TreeData;

class TreeFilterTest {

    private static TreeData.Node node(String type, String name, TreeData.Node... children) {
        return new TreeData.Node(type, name, "C:/proyecto/" + name, null, null, false, List.of(children));
    }

    private static List<TreeData.Node> sample() {
        TreeData.Node agent = node("agent", "analista-facturacion");
        TreeData.Node skill = node("skill", "team-conventions");
        TreeData.Node module = node("module", "facturacion", agent, skill);
        TreeData.Node global = node("root", "Global");
        TreeData.Node project = node("root", "Project", module);
        return List.of(global, project);
    }

    @Test
    void emptyQueryReturnsSameTree() {
        List<TreeData.Node> nodes = sample();
        assertSame(nodes, TreeFilter.filter(nodes, "  "));
        assertSame(nodes, TreeFilter.filter(nodes, null));
    }

    @Test
    void keepsMatchingLeafAndItsAncestors() {
        List<TreeData.Node> filtered = TreeFilter.filter(sample(), "team");

        assertEquals(1, filtered.size());
        TreeData.Node project = filtered.getFirst();
        assertEquals("Project", project.name());
        assertEquals(1, project.children().size());
        assertEquals("facturacion", project.children().getFirst().name());
        assertEquals(1, project.children().getFirst().children().size());
        assertEquals("team-conventions", project.children().getFirst().children().getFirst().name());
        assertTrue(TreeFilter.hasMatches(filtered));
        assertEquals(2, TreeFilter.countVisible(filtered));
    }

    @Test
    void matchesByTypeAndIsCaseInsensitive() {
        List<TreeData.Node> filtered = TreeFilter.filter(sample(), "AGENT");

        assertEquals(1, filtered.size());
        TreeData.Node module = filtered.getFirst().children().getFirst();
        assertEquals(1, module.children().size());
        assertEquals("analista-facturacion", module.children().getFirst().name());
    }

    @Test
    void matchingParentKeepsAllItsDescendants() {
        TreeData.Node provider = node("provider", "claude",
            node("agent", "analista"), node("agent", "desarrollador"));
        List<TreeData.Node> filtered = TreeFilter.filter(List.of(provider), "claude");

        assertEquals(1, filtered.size());
        assertEquals(2, filtered.getFirst().children().size());
        assertEquals(3, TreeFilter.countVisible(filtered));
    }

    @Test
    void noMatchesProducesEmptyTree() {
        List<TreeData.Node> filtered = TreeFilter.filter(sample(), "inexistente");

        assertTrue(filtered.isEmpty());
        assertFalse(TreeFilter.hasMatches(filtered));
        assertEquals(0, TreeFilter.countVisible(filtered));
    }
}
