package es.vargontoc.agentic.develop.cli.command;

import java.io.InputStream;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;

/**
 * Los comandos disponibles. Los servicios se piden de forma perezosa para que la ayuda y la versión funcionen
 * aunque el entorno no permita montarlos.
 */
public final class Commands {

    private Commands() {}

    public static CommandRegistry standard(Supplier<Services> services) {
        return standard(services, () -> System.in);
    }

    /** @param input entrada estándar; solo la lee {@code save --stdin}, cuando se pide de forma explícita */
    public static CommandRegistry standard(Supplier<Services> services, Supplier<InputStream> input) {
        return new CommandRegistry()
            .register(new InitCommand(services))
            .register(new ConfigureProviderCommand(services))
            .register(new StatusCommand(services))
            .register(new TreeCommand(services))
            .register(new DescribeCommand(services))
            .register(new CreateModuleCommand(services))
            .register(new AddTechnicianCommand(services))
            .register(new CreateArtifactCommand(services, input))
            .register(new SaveCommand(services, input));
    }
}
