package es.vargontoc.agentic.viewer.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.vargontoc.agentic.viewer.cli.model.TreeData;

/**
 * Filtra el árbol de artefactos por texto libre. Conserva los nodos que
 * coinciden y sus ancestros para no romper la jerarquía. Es lógica pura, sin
 * JavaFX, para poder probarla con JUnit.
 */
public final class TreeFilter {

    private TreeFilter() {}

    /** Devuelve el árbol filtrado; si la consulta está vacía, devuelve la lista original. */
    public static List<TreeData.Node> filter(List<TreeData.Node> nodes, String query) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return nodes;
        }
        return filterNodes(nodes, normalized);
    }

    /** Indica si hay alguna coincidencia en el árbol ya filtrado. */
    public static boolean hasMatches(List<TreeData.Node> filtered) {
        if (filtered == null) {
            return false;
        }
        for (TreeData.Node node : filtered) {
            if (node.children() != null && !node.children().isEmpty()) {
                return true;
            }
            if (!TreeModel.ROOT.equals(node.type())) {
                return true;
            }
        }
        return false;
    }

    /** Cuenta los nodos visibles que no son raíces de navegación. */
    public static int countVisible(List<TreeData.Node> nodes) {
        if (nodes == null) {
            return 0;
        }
        int count = 0;
        for (TreeData.Node node : nodes) {
            if (!TreeModel.ROOT.equals(node.type())) {
                count++;
            }
            count += countVisible(node.children());
        }
        return count;
    }

    private static List<TreeData.Node> filterNodes(List<TreeData.Node> nodes, String query) {
        List<TreeData.Node> result = new ArrayList<>();
        for (TreeData.Node node : nodes) {
            if (matches(node, query)) {
                result.add(copySubtree(node));
                continue;
            }
            List<TreeData.Node> children = filterNodes(node.children(), query);
            if (!children.isEmpty()) {
                result.add(copyWithChildren(node, children));
            }
        }
        return result;
    }

    private static boolean matches(TreeData.Node node, String query) {
        return contains(node.name(), query)
            || contains(node.type(), query)
            || contains(node.provider(), query)
            || contains(node.status(), query)
            || contains(node.path(), query);
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static TreeData.Node copyWithChildren(TreeData.Node node, List<TreeData.Node> children) {
        return new TreeData.Node(
            node.type(),
            node.name(),
            node.path(),
            node.provider(),
            node.status(),
            node.pending(),
            children);
    }

    private static TreeData.Node copySubtree(TreeData.Node node) {
        List<TreeData.Node> children = new ArrayList<>();
        if (node.children() != null) {
            for (TreeData.Node child : node.children()) {
                children.add(copySubtree(child));
            }
        }
        return copyWithChildren(node, children);
    }

    private static String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }
}
