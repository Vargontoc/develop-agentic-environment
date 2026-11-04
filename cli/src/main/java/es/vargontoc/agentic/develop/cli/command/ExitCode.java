package es.vargontoc.agentic.develop.cli.command;

/** Códigos de salida del modo no interactivo (REQ-005). */
public enum ExitCode {
    OK(0), DOMAIN_ERROR(1), USAGE_ERROR(2), INTERNAL_ERROR(3);

    private final int value;

    ExitCode(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
