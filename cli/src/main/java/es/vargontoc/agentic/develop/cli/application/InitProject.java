package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;

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
     * Valida la ruta (vacía = directorio actual), copia el recurso .sdd y recuerda el proyecto.
     */
    public Path start(String input) {
        Path dir = parse(input);

        if(!Files.exists(dir))
            throw new DomainException(AppErrorCode.PROJECT_PATH_NOT_FOUND);
        if(!Files.isDirectory(dir))
            throw new DomainException(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY);
        if(Files.exists(dir.resolve(SDD_DIR)))
            throw new DomainException(AppErrorCode.PROJECT_ALREADY_CONFIGURED);

        copySdd(dir.resolve(SDD_DIR));
        currentProject = dir;
        return dir;
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
        URL resource = InitProject.class.getResource(SDD_RESOURCE);
        if(resource == null) {
            log.error("Resource '{}' not found", SDD_RESOURCE);
            throw new DomainException(AppErrorCode.SDD_RESOURCE_NOT_FOUND);
        }

        try {
            URI uri = resource.toURI();

            if("jar".equals(uri.getScheme())) {
                try(FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                    copyTree(fs.getPath(SDD_RESOURCE), target);
                }
            }else {
                copyTree(Path.of(uri), target);
            }
        }catch(URISyntaxException | IOException e) {
            log.error(e.getMessage(), e);
            throw new DomainException(AppErrorCode.PROJECT_COPY_FAILED, e);
        }
    }

    private void copyTree(Path src, Path dest) throws IOException {
        try(Stream<Path> files = Files.walk(src)) {
            for(Path file : files.toList()) {
                Path target = dest.resolve(src.relativize(file).toString());
                if(Files.isDirectory(file))
                    Files.createDirectories(target);
                else
                    Files.copy(file, target);
            }
        }
    }
}
