package es.vargontoc.agentic.develop.cli.command;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.application.OperationResult;
import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/**
 * {@code configure-provider --provider <id>}: instala en la carpeta global del proveedor los agentes, skills y perfiles
 * de la aplicación. Nada existente se sobrescribe, así que repetirlo es seguro. Las rutas son relativas al proveedor.
 */
final class ConfigureProviderCommand implements Command {

    static final String PROVIDER = "provider";

    record ConfigureView(String provider, String path, List<String> created, List<String> skipped, List<String> warnings) {}

    private final Supplier<Services> services;

    ConfigureProviderCommand(Supplier<Services> services) {
        this.services = services;
    }

    @Override
    public String name() {
        return "configure-provider";
    }

    @Override
    public String description() {
        return "Instala los agentes y skills globales en un proveedor";
    }

    @Override
    public List<OptionSpec> options() {
        return List.of(OptionSpec.value(PROVIDER, true, "Proveedor: claude, codex, gemini u opencode"));
    }

    @Override
    public Object execute(ParsedArgs args) {
        Services s = services.get();
        String raw = args.required(PROVIDER);
        ProviderType type;
        Path home;
        try {
            type = ProviderType.fromId(raw);
            home = s.paths().providerHome(type);
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), raw, e);
        }

        OperationResult result = s.configureProvider().execute(new Provider(type, home), true);
        return new ConfigureView(type.id(), Views.text(home), result.created(), result.skipped(), result.warnings());
    }
}
