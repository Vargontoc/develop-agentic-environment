package es.vargontoc.agentic.develop.cli.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.vargontoc.agentic.develop.cli.application.OperationResult;
import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/** Opciones y forma de los datos que comparten {@code create-module} y {@code add-technician}. */
final class ModuleOptions {

    static final String MODULE = "module";
    static final String TECHNICIANS = "technicians";
    static final String PROVIDERS = "providers";
    static final String DRY_RUN = "dry-run";

    record ModuleView(String module, boolean dryRun, List<String> created, List<String> skipped, List<String> warnings) {}

    private ModuleOptions() {}

    static List<OptionSpec> specs(String moduleDescription) {
        return List.of(
            ProjectOption.spec(true),
            OptionSpec.value(MODULE, true, moduleDescription),
            OptionSpec.value(TECHNICIANS, true, "Técnicos separados por comas: analista, desarrollador, documentador"),
            OptionSpec.value(PROVIDERS, true, "Proveedores separados por comas: claude, codex, gemini, opencode"),
            OptionSpec.flag(DRY_RUN, "Devuelve lo previsto sin escribir nada"));
    }

    static List<Technician> technicians(ParsedArgs args) {
        List<Technician> result = new ArrayList<>();
        for(String item : items(args.required(TECHNICIANS), TECHNICIANS)) {
            Technician technician = java.util.Arrays.stream(Technician.values())
                .filter(t -> t.slug().equals(item.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new UsageException(UsageError.INVALID_VALUE, "--" + TECHNICIANS + " " + item));
            if(!result.contains(technician))
                result.add(technician);
        }
        return result;
    }

    /** Proveedores por su identificador; las carpetas de proyecto salen del propio proveedor, no de la ruta. */
    static List<Provider> providers(ParsedArgs args, Path project) {
        List<Provider> result = new ArrayList<>();
        for(String item : items(args.required(PROVIDERS), PROVIDERS)) {
            ProviderType type;
            try {
                type = ProviderType.fromId(item.toLowerCase(Locale.ROOT));
            } catch(DomainException e) {
                throw new PathDomainException(e.code(), item, e);
            }
            if(result.stream().noneMatch(p -> p.getType() == type))
                result.add(new Provider(type, project));
        }
        return result;
    }

    static ModuleView view(String module, boolean dryRun, OperationResult result) {
        return new ModuleView(module, dryRun, result.created(), result.skipped(), result.warnings());
    }

    // Una lista vacía o con elementos vacíos es un valor no válido, no un uso correcto
    private static List<String> items(String raw, String option) {
        List<String> items = java.util.Arrays.stream(raw.split(",", -1)).map(String::trim).toList();
        if(items.stream().anyMatch(String::isEmpty))
            throw new UsageException(UsageError.INVALID_VALUE, "--" + option + " " + raw);
        return items;
    }
}
