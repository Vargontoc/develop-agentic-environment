package es.vargontoc.agentic.viewer.tree;

import java.util.List;

import es.vargontoc.agentic.viewer.cli.model.TreeData;

/**
 * Convierte la respuesta de {@code tree} en dos raíces de navegación: Global y
 * Project. No modifica ni duplica nodos; el CLI ya entrega la estructura.
 */
public final class TreeModel {

    public static final String ROOT = "root";

    private TreeModel() {}

    public static List<TreeData.Node> roots(TreeData data) {
        return List.of(
            new TreeData.Node(ROOT, "Global", null, null, null, false, safe(data.global())),
            new TreeData.Node(ROOT, "Project", null, null, null, false, safe(data.project()))
        );
    }

    private static List<TreeData.Node> safe(List<TreeData.Node> nodes) {
        return nodes == null ? List.of() : List.copyOf(nodes);
    }
}
