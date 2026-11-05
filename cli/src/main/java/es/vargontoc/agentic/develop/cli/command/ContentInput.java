package es.vargontoc.agentic.develop.cli.command;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/**
 * El contenido que se le pasa a un comando de escritura: de un fichero ({@code --content-file}) o de la entrada
 * estándar ({@code --stdin}). La entrada estándar solo se toca si se pide de forma explícita.
 */
final class ContentInput {

    static final String CONTENT_FILE = "content-file";
    static final String STDIN = "stdin";

    private final Supplier<InputStream> input;

    ContentInput(Supplier<InputStream> input) {
        this.input = input;
    }

    static List<OptionSpec> specs() {
        return List.of(
            OptionSpec.value(CONTENT_FILE, false, "Fichero con el contenido"),
            OptionSpec.flag(STDIN, "Lee el contenido de la entrada estándar"));
    }

    /** @param required si es falso y no se indicó ninguna fuente, devuelve vacío */
    Optional<String> read(ParsedArgs args, boolean required) {
        boolean fromStdin = args.flag(STDIN);
        String file = args.optional(CONTENT_FILE).orElse(null);
        if(fromStdin && file != null)
            throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, "--" + STDIN);
        if(!fromStdin && file == null) {
            if(required)
                throw new UsageException(UsageError.MISSING_OPTION, "--" + CONTENT_FILE + " o --" + STDIN);
            return Optional.empty();
        }

        try {
            if(fromStdin)
                return Optional.of(new String(input.get().readAllBytes(), StandardCharsets.UTF_8));
            return Optional.of(Files.readString(Path.of(file), StandardCharsets.UTF_8));
        } catch(IOException | InvalidPathException e) {
            throw new PathDomainException(AppErrorCode.CONTENT_UNREADABLE, fromStdin ? STDIN : file, e);
        }
    }
}
