package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;

public class InitProject {

    private static final Logger log = LoggerFactory.getLogger(InitProject.class);

    private static final String SDD_DIR = ".sdd";
    private static final String SDD_RESOURCE = "/project/.sdd";

    private final PathResolver paths;

    // Ruta del proyecto activo; solo vive en memoria durante la sesión
    private Path currentProject;

    public InitProject() {
        this(PathResolver.system());
    }

    public InitProject(PathResolver paths) {
        this.paths = paths;
    }

    public Optional<Path> currentProject() {
        return Optional.ofNullable(currentProject);
    }

    /**
     * Valida la ruta (vacía = directorio actual) y recuerda el proyecto.
     * Si el directorio aún no tiene .sdd lo copia; si ya lo tiene, lo da por configurado sin tocarlo.
     */
    public ProjectStart start(String input) {
        Path dir = paths.resolveProject(input);

        boolean alreadyConfigured = Files.exists(dir.resolve(SDD_DIR));
        if(!alreadyConfigured)
            copySdd(dir.resolve(SDD_DIR));

        currentProject = dir;
        return new ProjectStart(dir, alreadyConfigured);
    }

    private void copySdd(Path target) {
        try {
            if(ClasspathResources.copyTreeIfAbsent(SDD_RESOURCE, target).isEmpty()) {
                log.error("Resource '{}' not found", SDD_RESOURCE);
                throw new DomainException(AppErrorCode.SDD_RESOURCE_NOT_FOUND);
            }
        }catch(IOException e) {
            log.error(e.getMessage(), e);
            throw new DomainException(AppErrorCode.PROJECT_COPY_FAILED, e);
        }
    }
}
