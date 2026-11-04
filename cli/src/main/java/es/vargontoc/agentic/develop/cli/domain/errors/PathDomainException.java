package es.vargontoc.agentic.develop.cli.domain.errors;

import java.nio.file.Path;

import es.vargontoc.framework.core.error.ErrorCode;

/**
 * Error de dominio que señala la ruta afectada (REQ-023). Los parámetros del modelo de errores del framework
 * no admiten rutas, así que se lleva aparte. Es texto y no un {@link Path} porque puede ser una entrada del usuario
 * que ni siquiera es una ruta válida (por ejemplo una ubicación dentro de un jar).
 */
public class PathDomainException extends DomainException {

    private final String location;

    public PathDomainException(ErrorCode code, String location) {
        super(code);
        this.location = location;
    }

    public PathDomainException(ErrorCode code, String location, Throwable cause) {
        super(code, cause);
        this.location = location;
    }

    public PathDomainException(ErrorCode code, Path path) {
        this(code, path.toString());
    }

    public PathDomainException(ErrorCode code, Path path, Throwable cause) {
        super(code, cause);
        this.location = path.toString();
    }

    public String location() {
        return location;
    }
}
