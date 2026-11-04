package es.vargontoc.agentic.develop.cli.command;

/**
 * Motivos de uso incorrecto de la línea de comandos, con códigos estables. Son propios de la CLI y no del modelo de
 * errores del framework: son errores del llamante, no del dominio, y terminan siempre con el código de salida 2.
 */
public enum UsageError {
    UNKNOWN_COMMAND("usage.unknown_command", "Comando desconocido"),
    UNKNOWN_OPTION("usage.unknown_option", "Opción desconocida"),
    MISSING_OPTION("usage.missing_option", "Falta una opción obligatoria"),
    MISSING_VALUE("usage.missing_value", "Falta el valor de la opción"),
    DUPLICATE_OPTION("usage.duplicate_option", "Opción repetida"),
    UNEXPECTED_ARGUMENT("usage.unexpected_argument", "Argumento inesperado");

    private final String value;
    private final String message;

    UsageError(String value, String message) {
        this.value = value;
        this.message = message;
    }

    public String value() {
        return value;
    }

    public String message() {
        return message;
    }
}
