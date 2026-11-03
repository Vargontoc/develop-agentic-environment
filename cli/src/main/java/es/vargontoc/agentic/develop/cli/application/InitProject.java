package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;

public class InitProject {

    private static final Logger log = LoggerFactory.getLogger(InitProject.class);

    private static final String SDD_DIR = ".sdd";
    private static final String SDD_RESOURCE = "/project/.sdd";

    // Ruta del proyecto activo; solo vive en memoria durante la sesión
    private Path currentProject;

    public Optional<Path> currentProject() {
        return Optional.ofNullable(currentProject);
    }

    /**
     * Valida la ruta (vacía = directorio actual) y recuerda el proyecto.
     * Si el directorio aún no tiene .sdd lo copia; si ya lo tiene, lo da por configurado sin tocarlo.
     */
    public ProjectStart start(String input) {
        Path dir = parse(input);

        if(!Files.exists(dir))
            throw new DomainException(AppErrorCode.PROJECT_PATH_NOT_FOUND);
        if(!Files.isDirectory(dir))
            throw new DomainException(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY);

        boolean alreadyConfigured = Files.exists(dir.resolve(SDD_DIR));
        if(!alreadyConfigured)
            copySdd(dir.resolve(SDD_DIR));

        currentProject = dir;
        return new ProjectStart(dir, alreadyConfigured);
    }

    // Admite rutas absolutas y relativas con separadores de Windows o Linux
    private Path parse(String input) {
        String raw = input == null ? "" : input.trim();
        if(raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\""))
            raw = raw.substring(1, raw.length() - 1).trim();

        if(raw.isEmpty())
            return Path.of("").toAbsolutePath();

        try {
            return Path.of(raw.replace('\\', '/')).toAbsolutePath().normalize();
        } catch(InvalidPathException e) {
            throw new DomainException(AppErrorCode.PROJECT_PATH_INVALID, e);
        }
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
