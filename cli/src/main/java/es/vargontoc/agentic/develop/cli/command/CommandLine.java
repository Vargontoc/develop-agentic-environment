package es.vargontoc.agentic.develop.cli.command;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.command.ArgumentParser.Help;
import es.vargontoc.agentic.develop.cli.command.ArgumentParser.Invocation;
import es.vargontoc.agentic.develop.cli.command.ArgumentParser.Run;
import es.vargontoc.agentic.develop.cli.command.ArgumentParser.VersionRequest;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador de entrada no interactivo (DEC-001, DEC-003): interpreta la línea de comandos, ejecuta el comando y escribe
 * un único envelope JSON en la salida de datos. Los logs y las trazas van solo por el logger (salida de error).
 * No lee de la entrada estándar ni termina el proceso: devuelve el código de salida.
 */
public final class CommandLine {

    private static final Logger log = LoggerFactory.getLogger(CommandLine.class);

    static final String HELP_COMMAND = "help";
    static final String VERSION_COMMAND = "version";

    private final CommandRegistry registry;
    private final Supplier<String> version;
    private final JsonMapper mapper = JsonMapperFactory.create();

    public CommandLine(CommandRegistry registry) {
        this(registry, Version::current);
    }

    public CommandLine(CommandRegistry registry, Supplier<String> version) {
        this.registry = registry;
        this.version = version;
    }

    /**
     * @param args argumentos, al menos uno
     * @param out  salida de datos: recibe exactamente un documento JSON, en una línea
     * @return código de salida
     */
    public int run(String[] args, PrintStream out) {
        String command = args.length > 0 ? args[0] : null;
        ExitCode exit = ExitCode.OK;
        String json;

        try {
            Invocation invocation = new ArgumentParser(registry).parse(args);
            command = nameOf(invocation);
            // La serialización va dentro del try: unos datos no serializables son un fallo interno, no un envelope roto
            json = mapper.writeValueAsString(Envelope.success(command, execute(invocation)));
        } catch(RuntimeException | StackOverflowError failure) {
            ErrorMapper.Mapped mapped = ErrorMapper.map(failure);
            exit = mapped.exit();
            json = mapper.writeValueAsString(Envelope.failure(command, mapped.error()));
            report(failure, mapped);
        }

        out.println(json);
        out.flush();
        return exit.value();
    }

    private static String nameOf(Invocation invocation) {
        return switch(invocation) {
            case Help help -> HELP_COMMAND;
            case VersionRequest request -> VERSION_COMMAND;
            case Run run -> run.command().name();
        };
    }

    private Object execute(Invocation invocation) {
        return switch(invocation) {
            case Help help -> helpData(help);
            case VersionRequest request -> Map.of("version", version.get());
            case Run run -> run.command().execute(run.args());
        };
    }

    private Object helpData(Help help) {
        if(help.command() != null)
            return Map.of("commands", List.of(describe(help.command())));
        return Map.of("commands", registry.all().stream().map(CommandLine::describe).toList(),
            "globalOptions", List.of(
                Map.of("name", "help", "description", "Muestra la ayuda"),
                Map.of("name", "version", "description", "Muestra la versión")));
    }

    private static Map<String, Object> describe(Command command) {
        return Map.of("name", command.name(), "description", command.description(),
            "options", command.options().stream()
                .map(o -> Map.of("name", o.name(), "flag", o.flag(), "required", o.required(), "description", o.description()))
                .toList());
    }

    // Los fallos esperados no llevan traza; los internos la escriben en el log (salida de error), nunca en los datos
    private static void report(Throwable failure, ErrorMapper.Mapped mapped) {
        if(mapped.exit() == ExitCode.INTERNAL_ERROR)
            log.error("{}: {}", mapped.error().code(), failure.getMessage(), failure);
        else
            log.info("{}: {}", mapped.error().code(), mapped.error().message());
    }
}
