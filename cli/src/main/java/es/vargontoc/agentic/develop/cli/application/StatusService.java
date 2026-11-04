package es.vargontoc.agentic.develop.cli.application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.domain.ProviderStatus;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.team.TeamInfo;
import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning;

/**
 * Estado de los proveedores y, si se indica, del proyecto. Solo lee.
 */
public class StatusService {

    /** @param initialized true si el proyecto ya tiene {@code .sdd} */
    public record ProjectStatus(Path path, boolean initialized, List<String> modules, List<TeamWarning> warnings) {
        public ProjectStatus {
            modules = List.copyOf(modules);
            warnings = List.copyOf(warnings);
        }
    }

    /** @param project null si no se pidió un proyecto */
    public record StatusReport(List<ProviderStatus> providers, ProjectStatus project) {
        public StatusReport {
            providers = List.copyOf(providers);
        }
    }

    private final ProviderCatalog providers;
    private final ProjectWorkspace workspace;

    public StatusService(ProviderCatalog providers, ProjectWorkspace workspace) {
        this.providers = providers;
        this.workspace = workspace;
    }

    /** @param project proyecto a incluir, o null para informar solo de los proveedores */
    public StatusReport status(Path project) {
        List<ProviderStatus> statuses = List.of(ProviderType.values()).stream().map(providers::status).toList();
        return new StatusReport(statuses, project == null ? null : projectStatus(project));
    }

    private ProjectStatus projectStatus(Path project) {
        if(!Files.exists(project))
            throw new PathDomainException(AppErrorCode.PROJECT_PATH_NOT_FOUND, project);
        if(!Files.isDirectory(project))
            throw new PathDomainException(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY, project);

        TeamInfo team = workspace.readTeam(project);
        return new ProjectStatus(project, Files.isDirectory(project.resolve(ProjectWorkspace.SDD_DIR)),
            team.moduleNames(), team.warnings());
    }
}
