package es.vargontoc.agentic.develop.cli.command;

/**
 * Opción de un comando: {@code --nombre valor}, {@code --nombre=valor} o, si es un indicador, {@code --nombre}.
 */
public record OptionSpec(String name, boolean flag, boolean required, String description) {

    public static OptionSpec value(String name, boolean required, String description) {
        return new OptionSpec(name, false, required, description);
    }

    public static OptionSpec flag(String name, String description) {
        return new OptionSpec(name, true, false, description);
    }
}
