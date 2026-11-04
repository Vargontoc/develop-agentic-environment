package es.vargontoc.agentic.develop.cli.command;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import es.vargontoc.agentic.develop.cli.application.StatusService.ProjectStatus;
import es.vargontoc.agentic.develop.cli.application.StatusService.StatusReport;
import es.vargontoc.agentic.develop.cli.domain.ProviderStatus;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactDescription;
import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning;
import es.vargontoc.agentic.develop.cli.domain.tree.ProjectTree;
import es.vargontoc.agentic.develop.cli.domain.tree.TreeNode;

/**
 * Forma de los datos que salen en el campo {@code data} del envelope. Es el contrato con el viewer: rutas como texto
 * con {@code /}, enumeraciones en minúsculas con identificadores estables y los campos sin valor presentes como null.
 */
final class Views {

    private Views() {}

    record WarningView(String type, int line) {}

    record ProviderView(String id, String name, String state, List<String> missing) {}

    record ProjectView(String path, boolean initialized, List<String> modules, List<WarningView> warnings) {}

    record StatusView(List<ProviderView> providers, ProjectView project) {}

    record NodeView(String type, String name, String path, String provider, String status, boolean pending,
                    List<NodeView> children) {}

    record ProblemView(String path, String code) {}

    record TreeView(List<NodeView> global, List<NodeView> project, List<WarningView> warnings, List<ProblemView> problems) {}

    record DescriptionView(String provider, String scope, String kind, String name, String path, String format,
                           String module, String header, String body, String sharedBody, String hash, String status,
                           boolean modifiedExternally, List<String> files) {}

    static StatusView of(StatusReport report) {
        return new StatusView(report.providers().stream().map(Views::of).toList(),
            report.project() == null ? null : of(report.project()));
    }

    static TreeView of(ProjectTree tree) {
        return new TreeView(tree.global().stream().map(Views::of).toList(), tree.project().stream().map(Views::of).toList(),
            tree.warnings().stream().map(Views::of).toList(),
            tree.problems().stream().map(p -> new ProblemView(text(p.path()), p.code().value())).toList());
    }

    static DescriptionView of(ArtifactDescription d) {
        return new DescriptionView(id(d.provider().id()), id(d.scope()), id(d.kind()), d.name(), text(d.path()),
            d.format() == null ? null : id(d.format()), d.module(), d.header(), d.body(), d.sharedBody(), d.hash(),
            id(d.status()), d.modifiedExternally(), d.files());
    }

    private static ProviderView of(ProviderStatus status) {
        return new ProviderView(status.type().id(), status.type().toString(), id(status.state()), status.missing());
    }

    private static ProjectView of(ProjectStatus project) {
        return new ProjectView(text(project.path()), project.initialized(), project.modules(),
            project.warnings().stream().map(Views::of).toList());
    }

    private static WarningView of(TeamWarning warning) {
        return new WarningView(id(warning.type()), warning.line());
    }

    private static NodeView of(TreeNode node) {
        return new NodeView(id(node.type()), node.name(), text(node.path()),
            node.provider() == null ? null : node.provider().id(), node.status() == null ? null : id(node.status()),
            node.pending(), node.children().stream().map(Views::of).toList());
    }

    private static String id(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static String id(String value) {
        return value;
    }

    /** Rutas siempre con {@code /}, también en Windows, para que el contrato no dependa del sistema. */
    static String text(Path path) {
        return path == null ? null : path.toString().replace('\\', '/');
    }
}
