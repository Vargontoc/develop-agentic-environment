package es.vargontoc.agentic.develop.cli.application;

import java.nio.file.Path;
import java.util.Optional;

import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

/**
 * Inicio de proyecto del menú interactivo: resuelve la ruta, inicializa el espacio de trabajo y recuerda el proyecto activo.
 * El estado en memoria es solo de la sesión del menú; las operaciones en sí están en {@link ProjectWorkspace}.
 */
public class InitProject {

    private final PathResolver paths;
    private final ProjectWorkspace workspace;

    // Ruta del proyecto activo; solo vive en memoria durante la sesión
    private Path currentProject;

    public InitProject() {
        this(PathResolver.system());
    }

    public InitProject(PathResolver paths) {
        this(paths, new ProjectWorkspace(paths, paths.appHome().resolve("backups")));
    }

    public InitProject(PathResolver paths, ProjectWorkspace workspace) {
        this.paths = paths;
        this.workspace = workspace;
    }

    public Optional<Path> currentProject() {
        return Optional.ofNullable(currentProject);
    }

    /**
     * Valida la ruta (vacía = directorio actual) y recuerda el proyecto.
     * Si el directorio aún no tiene .sdd lo copia; si ya lo tiene, lo da por configurado sin tocarlo.
     */
    public ProjectStart start(String input) {
        ProjectWorkspace.InitResult result = workspace.initialize(paths.resolveProject(input));

        currentProject = result.path();
        return new ProjectStart(result.path(), result.alreadyConfigured());
    }
}
