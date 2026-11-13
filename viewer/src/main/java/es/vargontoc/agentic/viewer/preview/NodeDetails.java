package es.vargontoc.agentic.viewer.preview;

import java.util.ArrayList;
import java.util.List;

import es.vargontoc.agentic.viewer.cli.model.TeamData;
import es.vargontoc.agentic.viewer.cli.model.TreeData;
import es.vargontoc.agentic.viewer.ui.TreePresentation;

/** Construye la ficha de módulos y técnicos a partir del árbol y, si está, de {@code team.md}. */
public final class NodeDetails {

    private NodeDetails() {}

    public static NodeDetail module(TreeData.Node node, TeamData team) {
        String moduleName = node.name();
        TeamData.Module module = team == null ? null : team.modules().stream()
            .filter(m -> moduleName.equals(m.name()))
            .findFirst()
            .orElse(null);

        List<NodeDetail.Item> info = new ArrayList<>();
        info.add(new NodeDetail.Item("Nombre", "Registrado en .sdd/team.md",
            module == null ? "sin datos" : "registrado", null));
        if (module != null) {
            info.add(new NodeDetail.Item("Identificador",
                module.mapId() == null ? "Sin identificador" : module.mapId(), null, null));
            info.add(new NodeDetail.Item("Rutas",
                module.paths().isEmpty() ? "Sin rutas" : String.join(", ", module.paths()), null, null));
        }

        List<TreeData.Node> technicians = node.children() == null ? List.of() : node.children();
        List<NodeDetail.Item> techItems = new ArrayList<>();
        for (TreeData.Node technician : technicians) {
            int artifacts = technician.children() == null ? 0 : technician.children().size();
            techItems.add(new NodeDetail.Item(technician.name(),
                artifacts + (artifacts == 1 ? " artefacto" : " artefactos"),
                technician.pending() ? "pendiente" : "instalado", null));
        }

        List<NodeDetail.Item> artifactItems = new ArrayList<>();
        for (TreeData.Node technician : technicians) {
            if (technician.children() == null) {
                continue;
            }
            for (TreeData.Node artifact : technician.children()) {
                artifactItems.add(artifact(artifact, technician.name()));
            }
        }

        List<NodeDetail.Section> sections = new ArrayList<>();
        sections.add(new NodeDetail.Section("Módulo", info));
        sections.add(new NodeDetail.Section("Técnicos (" + technicians.size() + ")", techItems));
        sections.add(new NodeDetail.Section("Artefactos (" + artifactItems.size() + ")", artifactItems));
        String subtitle = "Módulo del equipo · "
            + technicians.size() + (technicians.size() == 1 ? " técnico · " : " técnicos · ")
            + artifactItems.size() + (artifactItems.size() == 1 ? " artefacto" : " artefactos");
        return new NodeDetail(moduleName, subtitle, sections);
    }

    public static NodeDetail technician(TreeData.Node node, TreeData.Node parent) {
        String moduleName = parent == null ? "—" : parent.name();
        List<TreeData.Node> artifacts = node.children() == null ? List.of() : node.children();

        List<NodeDetail.Item> info = new ArrayList<>();
        info.add(new NodeDetail.Item("Rol", "Técnico del módulo " + moduleName,
            node.pending() ? "pendiente" : "instalado", null));
        info.add(new NodeDetail.Item("Módulo", moduleName, null, null));
        info.add(new NodeDetail.Item("Artefactos",
            artifacts.size() + (artifacts.size() == 1 ? " instalado" : " instalados"), null, null));

        List<NodeDetail.Item> artifactItems = new ArrayList<>();
        for (TreeData.Node artifact : artifacts) {
            artifactItems.add(artifact(artifact, moduleName));
        }

        List<NodeDetail.Section> sections = new ArrayList<>();
        sections.add(new NodeDetail.Section("Técnico", info));
        sections.add(new NodeDetail.Section("Artefactos (" + artifacts.size() + ")", artifactItems));
        return new NodeDetail(node.name(), "Técnico · módulo " + moduleName, sections);
    }

    private static NodeDetail.Item artifact(TreeData.Node artifact, String context) {
        String provider = artifact.provider() == null ? "" : " · " + artifact.provider();
        String detail = TreePresentation.kindLabel(artifact.type()) + provider
            + (context == null || context.isBlank() ? "" : " · " + context);
        String status = artifact.pending()
            ? "pendiente"
            : TreePresentation.statusLabel(artifact.status());
        return new NodeDetail.Item(artifact.name(), detail, status, artifact.path());
    }
}
