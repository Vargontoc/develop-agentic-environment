package es.vargontoc.agentic.viewer.cli;

/**
 * Error del cliente del CLI. Conserva el código estable, la ruta afectada y el
 * mensaje original para que la interfaz pueda presentarlo y sugerir una acción.
 */
public final class CliClientException extends RuntimeException {

    private final String code;
    private final String path;
    private final int exitCode;

    public CliClientException(String code, String path, String message, int exitCode) {
        super(message == null || message.isBlank() ? code : message);
        this.code = code;
        this.path = path;
        this.exitCode = exitCode;
    }

    public String code() {
        return code;
    }

    public String path() {
        return path;
    }

    public int exitCode() {
        return exitCode;
    }
}
