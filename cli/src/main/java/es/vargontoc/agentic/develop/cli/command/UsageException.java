package es.vargontoc.agentic.develop.cli.command;

/**
 * Uso incorrecto de la línea de comandos. Distinta de {@code DomainException}: la CLI la traduce al código de salida 2.
 *
 * @see UsageError
 */
public class UsageException extends RuntimeException {

    private final transient UsageError error;
    private final String detail;

    /** @param detail lo que el usuario escribió y causó el error (comando u opción); puede ser null */
    public UsageException(UsageError error, String detail) {
        super(detail == null ? error.message() : error.message() + ": " + detail);
        this.error = error;
        this.detail = detail;
    }

    public UsageError error() {
        return error;
    }

    public String detail() {
        return detail;
    }
}
