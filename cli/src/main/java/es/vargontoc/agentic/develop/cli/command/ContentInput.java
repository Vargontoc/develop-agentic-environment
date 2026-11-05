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
 * Un texto que se le pasa a un comando de escritura: de un fichero ({@code --<nombre>-file}) o de la entrada estándar
 * ({@code --<nombre>-stdin}). La entrada estándar solo se toca si se pide de forma explícita.
 */
final class ContentInput {

    static final String CONTENT_FILE = "content-file";
    static final String STDIN = "stdin";

    private final Supplier<InputStream> input;
    private final String fileOption;
    private final String stdinOption;
    private final String what;

    /** El contenido principal: {@code --content-file} y {@code --stdin}. */
    ContentInput(Supplier<InputStream> input) {
        this(input, CONTENT_FILE, STDIN, "el contenido");
    }

    ContentInput(Supplier<InputStream> input, String fileOption, String stdinOption, String what) {
        this.input = input;
        this.fileOption = fileOption;
        this.stdinOption = stdinOption;
        this.what = what;
    }

    List<OptionSpec> options() {
        return List.of(
            OptionSpec.value(fileOption, false, "Fichero con " + what),
            OptionSpec.flag(stdinOption, "Lee " + what + " de la entrada estándar"));
    }

    /** @return verdadero si se pidió alguna fuente, sin leerla */
    boolean given(ParsedArgs args) {
        return args.flag(stdinOption) || args.optional(fileOption).isPresent();
    }

    boolean fromStdin(ParsedArgs args) {
        return args.flag(stdinOption);
    }

    /** @param required si es falso y no se indicó ninguna fuente, devuelve vacío */
    Optional<String> read(ParsedArgs args, boolean required) {
        boolean fromStdin = args.flag(stdinOption);
        String file = args.optional(fileOption).orElse(null);
        if(fromStdin && file != null)
            throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, "--" + stdinOption);
        if(!fromStdin && file == null) {
            if(required)
                throw new UsageException(UsageError.MISSING_OPTION, "--" + fileOption + " o --" + stdinOption);
            return Optional.empty();
        }

        try {
            if(fromStdin)
                return Optional.of(new String(input.get().readAllBytes(), StandardCharsets.UTF_8));
            return Optional.of(Files.readString(Path.of(file), StandardCharsets.UTF_8));
        } catch(IOException | InvalidPathException e) {
            throw new PathDomainException(AppErrorCode.CONTENT_UNREADABLE, fromStdin ? stdinOption : file, e);
        }
    }
}
