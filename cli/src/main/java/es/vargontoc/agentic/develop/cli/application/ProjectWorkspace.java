package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.team.TeamInfo;
import es.vargontoc.agentic.develop.cli.domain.team.TeamParser;
import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;

/**
 * La estructura {@code .sdd} de un proyecto: inicializarla y leer los módulos registrados en {@code team.md}.
 * No guarda estado entre llamadas: el proyecto se indica en cada operación.
 */
public class ProjectWorkspace {

    public record InitResult(Path path, boolean alreadyConfigured) {}

    private static final Logger log = LoggerFactory.getLogger(ProjectWorkspace.class);

    static final String SDD_DIR = ".sdd";
    static final String TEAM_FILE = "team.md";
    private static final String SDD_RESOURCE = "/project/.sdd";

    private final PathResolver paths;
    private final Path backupRoot;

    public ProjectWorkspace(PathResolver paths, Path backupRoot) {
        this.paths = paths;
        this.backupRoot = backupRoot;
    }

    /**
     * Copia la estructura {@code .sdd} si el proyecto aún no la tiene; si la tiene no toca nada.
     */
    public InitResult initialize(Path project) {
        if(!Files.exists(project))
            throw new DomainException(AppErrorCode.PROJECT_PATH_NOT_FOUND);
        if(!Files.isDirectory(project))
            throw new DomainException(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY);

        Path sdd = project.resolve(SDD_DIR);
        if(Files.exists(sdd))
            return new InitResult(project, true);

        try {
            if(ClasspathResources.copyTreeIfAbsent(SDD_RESOURCE, sdd).isEmpty()) {
                log.error("Resource '{}' not found", SDD_RESOURCE);
                throw new DomainException(AppErrorCode.SDD_RESOURCE_NOT_FOUND);
            }
        } catch(IOException e) {
            log.error(e.getMessage(), e);
            throw new DomainException(AppErrorCode.PROJECT_COPY_FAILED, e);
        }
        return new InitResult(project, false);
    }

    /**
     * Módulos registrados en {@code .sdd/team.md}. Nunca falla por el contenido del fichero: ante un fichero ausente,
     * ilegible o mal formado devuelve avisos y los módulos que sí se puedan leer.
     */
    public TeamInfo readTeam(Path project) {
        Path team = project.resolve(SDD_DIR).resolve(TEAM_FILE);
        if(!Files.isRegularFile(team))
            return TeamInfo.withWarning(new TeamWarning(TeamWarning.Type.TEAM_MISSING, 0));

        try {
            String content = new TextStore(paths, List.of(project), backupRoot).read(team).content();
            return TeamParser.parse(content);
        } catch(DomainException e) {
            log.warn("team.md unreadable: {}", e.code().value());
            return TeamInfo.withWarning(new TeamWarning(TeamWarning.Type.TEAM_UNREADABLE, 0));
        }
    }
}
