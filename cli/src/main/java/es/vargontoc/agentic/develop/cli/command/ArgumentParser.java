package es.vargontoc.agentic.develop.cli.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Interpreta la línea de comandos: {@code <comando> [--opcion valor | --opcion=valor | --indicador]}, más
 * {@code --help}, {@code -h} y {@code --version}. Cualquier otra cosa es un {@link UsageException}.
 */
public final class ArgumentParser {

    /** Lo que hay que hacer con la línea de comandos. */
    public sealed interface Invocation permits Help, VersionRequest, Run {}

    /** @param command comando del que se pide ayuda; null para la ayuda general */
    public record Help(Command command) implements Invocation {}

    public record VersionRequest() implements Invocation {}

    public record Run(Command command, ParsedArgs args) implements Invocation {}

    private static final String OPTION_PREFIX = "--";

    private final CommandRegistry registry;

    public ArgumentParser(CommandRegistry registry) {
        this.registry = registry;
    }

    public Invocation parse(String[] args) {
        String first = args[0];

        if(isHelp(first) || "--version".equals(first)) {
            if(args.length > 1)
                throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, args[1]);
            return isHelp(first) ? new Help(null) : new VersionRequest();
        }
        if(first.startsWith("-"))
            throw new UsageException(UsageError.UNKNOWN_OPTION, first);

        Command command = registry.find(first).orElseThrow(() -> new UsageException(UsageError.UNKNOWN_COMMAND, first));
        for(int i = 1; i < args.length; i++) {
            if(isHelp(args[i]))
                return new Help(command);
        }
        return new Run(command, parseOptions(command, args));
    }

    private static boolean isHelp(String token) {
        return "--help".equals(token) || "-h".equals(token);
    }

    private ParsedArgs parseOptions(Command command, String[] args) {
        Map<String, String> values = new HashMap<>();
        Set<String> flags = new HashSet<>();

        int i = 1;
        while(i < args.length) {
            String token = args[i];
            if(!token.startsWith(OPTION_PREFIX))
                throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, token);

            int equals = token.indexOf('=');
            String name = equals < 0 ? token.substring(OPTION_PREFIX.length()) : token.substring(OPTION_PREFIX.length(), equals);
            OptionSpec spec = find(command, name).orElseThrow(() -> new UsageException(UsageError.UNKNOWN_OPTION, OPTION_PREFIX + name));

            if(spec.flag()) {
                if(equals >= 0)
                    throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, token);
                if(!flags.add(name))
                    throw new UsageException(UsageError.DUPLICATE_OPTION, OPTION_PREFIX + name);
                i++;
                continue;
            }

            String value;
            if(equals >= 0) {
                value = token.substring(equals + 1);
                i++;
            } else {
                value = i + 1 < args.length && !args[i + 1].startsWith(OPTION_PREFIX) ? args[i + 1] : "";
                i += 2;
            }
            if(value.isEmpty())
                throw new UsageException(UsageError.MISSING_VALUE, OPTION_PREFIX + name);
            if(values.putIfAbsent(name, value) != null)
                throw new UsageException(UsageError.DUPLICATE_OPTION, OPTION_PREFIX + name);
        }

        for(OptionSpec spec : command.options()) {
            if(spec.required() && !values.containsKey(spec.name()))
                throw new UsageException(UsageError.MISSING_OPTION, OPTION_PREFIX + spec.name());
        }
        return new ParsedArgs(values, flags);
    }

    private static Optional<OptionSpec> find(Command command, String name) {
        return command.options().stream().filter(o -> o.name().equals(name)).findFirst();
    }
}
