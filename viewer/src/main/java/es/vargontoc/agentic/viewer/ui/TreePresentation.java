package es.vargontoc.agentic.viewer.ui;

import java.util.ArrayList;
import java.util.List;

import es.vargontoc.agentic.viewer.cli.model.TreeData;
import es.vargontoc.agentic.viewer.tree.TreeGroups;
import javafx.scene.Node;

/**
 * Presentación común de los nodos del árbol: icono según el tipo y texto
 * emergente con la información útil.
 */
public final class TreePresentation {

    private TreePresentation() {}

    public static Node icon(TreeData.Node node) {
        String type = node.type() == null ? "" : node.type();
        Icons.Icon icon = switch (type) {
            case "agent" -> Icons.Icon.AGENT;
            case "skill" -> Icons.Icon.SKILL;
            case "hook" -> Icons.Icon.HOOK;
            case "profile" -> Icons.Icon.PROFILE;
            case "provider" -> Icons.Icon.PROVIDER;
            case "module" -> Icons.Icon.MODULE;
            case "technician" -> Icons.Icon.TECHNICIAN;
            case "sdd" -> Icons.Icon.SDD;
            case "group" -> Icons.Icon.FOLDER;
            case "root" -> "Global".equals(node.name()) ? Icons.Icon.GLOBE : Icons.Icon.FOLDER;
            default -> Icons.Icon.FOLDER;
        };
        List<String> classes = new ArrayList<>();
        classes.add("tree-icon");
        classes.add("icon-" + (type.isBlank() ? "unknown" : type));
        if (node.pending()) {
            classes.add("icon-pending");
        }
        if ("legacy".equals(node.status())) {
            classes.add("icon-legacy");
        }
        if ("detached".equals(node.status())) {
            classes.add("icon-detached");
        }
        return Icons.of(icon, 15, classes.toArray(String[]::new));
    }

    public static String kindLabel(String type) {
        if (type == null) {
            return "Nodo";
        }
        return switch (type) {
            case "agent" -> "Agente";
            case "skill" -> "Skill";
            case "profile" -> "Perfil";
            case "hook" -> "Hook";
            case "module" -> "Módulo";
            case "technician" -> "Técnico";
            case "sdd" -> "Directorio .sdd";
            case "provider" -> "Proveedor";
            case "group" -> "Carpeta";
            case "root" -> "Raíz";
            default -> type;
        };
    }

    public static String statusLabel(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "linked" -> "vinculado";
            case "legacy" -> "personalizado";
            case "detached" -> "desvinculado";
            default -> status;
        };
    }

    public static String tooltip(TreeData.Node node, boolean dirty, boolean stale) {
        if (TreeGroups.GROUP.equals(node.type())) {
            int count = node.children() == null ? 0 : node.children().size();
            return node.name() + " · " + count + (count == 1 ? " elemento" : " elementos");
        }
        StringBuilder text = new StringBuilder(kindLabel(node.type()));
        if (node.status() != null) {
            text.append(" · ").append(statusLabel(node.status()));
        }
        if (node.pending()) {
            text.append(" · pendiente");
        }
        if (node.path() != null) {
            text.append('\n').append(node.path());
        }
        if (dirty) {
            text.insert(0, "Cambios sin guardar\n");
        }
        if (stale) {
            text.append("\nDesactualizado: hay cambios externos");
        }
        return text.toString();
    }
}
