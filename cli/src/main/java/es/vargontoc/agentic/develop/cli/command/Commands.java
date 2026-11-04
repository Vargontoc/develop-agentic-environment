package es.vargontoc.agentic.develop.cli.command;

import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;

/**
 * Los comandos disponibles. Los servicios se piden de forma perezosa para que la ayuda y la versión funcionen
 * aunque el entorno no permita montarlos.
 */
public final class Commands {

    private Commands() {}

    public static CommandRegistry standard(Supplier<Services> services) {
        return new CommandRegistry()
            .register(new InitCommand(services))
            .register(new StatusCommand(services))
            .register(new TreeCommand(services))
            .register(new DescribeCommand(services));
    }
}
