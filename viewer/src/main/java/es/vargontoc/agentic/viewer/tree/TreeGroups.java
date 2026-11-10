package es.vargontoc.agentic.viewer.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import es.vargontoc.agentic.viewer.cli.model.TreeData;

/**
 * Agrupa los artefactos de un proveedor o técnico en carpetas por tipo para
 * que el árbol sea más fácil de leer. Es una transformación de presentación:
 * no modifica el árbol que devuelve el CLI.
 */
public final class TreeGroups {

    public static final String GROUP = "group";
    private static final int MIN_ARTIFACTS_TO_GROUP = 2;

    private static final List<Group> GROUPS = List.of(
        new Group("agent", "Agentes"),
        new Group("skill", "Skills"),
        new Group("profile", "Perfiles"),
        new Group("hook", "Hooks"));

    private record Group(String type, String label) {}

    private TreeGroups() {}

    public static List<TreeData.Node> group(List<TreeData.Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        return nodes.stream().map(TreeGroups::groupNode).toList();
    }

    private static TreeData.Node groupNode(TreeData.Node node) {
        List<TreeData.Node> children = node.children() == null
            ? List.of()
            : node.children().stream().map(TreeGroups::groupNode).toList();
        if (shouldGroup(node, children)) {
            children = groupByType(node, children);
        }
        return copy(node, children);
    }

    private static boolean shouldGroup(TreeData.Node parent, List<TreeData.Node> children) {
        if (!"provider".equals(parent.type()) && !"technician".equals(parent.type())) {
            return false;
        }
        return children.stream().filter(child -> typeGroup(child.type()) != null).count()
            >= MIN_ARTIFACTS_TO_GROUP;
    }

    private static List<TreeData.Node> groupByType(TreeData.Node parent, List<TreeData.Node> children) {
        Map<String, List<TreeData.Node>> byType = new LinkedHashMap<>();
        List<TreeData.Node> others = new ArrayList<>();
        for (TreeData.Node child : children) {
            String groupType = typeGroup(child.type());
            if (groupType == null) {
                others.add(child);
            } else {
                byType.computeIfAbsent(groupType, key -> new ArrayList<>()).add(child);
            }
        }

        List<TreeData.Node> result = new ArrayList<>(others);
        for (Group group : GROUPS) {
            List<TreeData.Node> items = byType.get(group.type());
            if (items == null || items.isEmpty()) {
                continue;
            }
            String provider = items.stream()
                .map(TreeData.Node::provider)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(parent.provider());
            String path = groupPath(parent, items, group.type());
            result.add(new TreeData.Node(GROUP, group.label(), path, provider, null, false, items));
        }
        return result;
    }

    private static String typeGroup(String type) {
        for (Group group : GROUPS) {
            if (group.type().equals(type)) {
                return type;
            }
        }
        return null;
    }

    private static String groupPath(TreeData.Node parent, List<TreeData.Node> items, String type) {
        String base = parent.path();
        if (base == null) {
            base = items.stream()
                .map(TreeData.Node::path)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        }
        return base == null ? null : base + "/#" + type;
    }

    private static TreeData.Node copy(TreeData.Node node, List<TreeData.Node> children) {
        return new TreeData.Node(
            node.type(),
            node.name(),
            node.path(),
            node.provider(),
            node.status(),
            node.pending(),
            children);
    }
}
