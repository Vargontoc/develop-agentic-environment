package es.vargontoc.agentic.viewer.tree;

import es.vargontoc.agentic.viewer.cli.model.TreeData;

/**
 * Texto visible de un nodo del árbol. Mantiene las marcas de pendiente,
 * personalizado y desvinculado sin acoplar la lógica a JavaFX.
 */
public final class TreeLabels {

    private TreeLabels() {}

    public static String label(TreeData.Node node) {
        return switch (node.type()) {
            case "technician" -> node.name() + (node.pending() ? " (pendiente)" : "");
            case "agent", "skill", "profile" -> node.name() + statusSuffix(node.status());
            case "hook" -> node.name() + (node.pending() ? " (pendiente)" : "") + statusSuffix(node.status());
            default -> node.name();
        };
    }

    private static String statusSuffix(String status) {
        if ("legacy".equals(status)) {
            return " [personalizado]";
        }
        if ("detached".equals(status)) {
            return " [desvinculado]";
        }
        return "";
    }
}
