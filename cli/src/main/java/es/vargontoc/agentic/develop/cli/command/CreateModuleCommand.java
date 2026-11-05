package es.vargontoc.agentic.develop.cli.command;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.application.OperationResult;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/**
 * {@code create-module --project <ruta> --module <nombre> --technicians <lista> --providers <lista> [--dry-run]}:
 * registra el módulo en {@code .sdd/team.md} y genera sus agentes y skills. Nada existente se sobrescribe, así que
 * repetirlo es seguro. Con {@code --dry-run} devuelve lo previsto sin escribir.
 */
final class CreateModuleCommand implements Command {

    private final Supplier<Services> services;

    CreateModuleCommand(Supplier<Services> services) {
        this.services = services;
    }

    @Override
    public String name() {
        return "create-module";
    }

    @Override
    public String description() {
        return "Crea un módulo de proyecto con los agentes de sus técnicos";
    }

    @Override
    public List<OptionSpec> options() {
        return ModuleOptions.specs("Nombre del módulo");
    }

    @Override
    public Object execute(ParsedArgs args) {
        Services s = services.get();
        Path project = ProjectOption.resolve(s, args);
        var technicians = ModuleOptions.technicians(args);
        var providers = ModuleOptions.providers(args, project);
        String module = args.required(ModuleOptions.MODULE);
        boolean dryRun = args.flag(ModuleOptions.DRY_RUN);

        OperationResult result;
        try {
            result = s.createModule().execute(project, module, technicians, providers, dryRun);
        } catch(PathDomainException e) {
            throw e;
        } catch(DomainException e) {
            // Los errores de nombre señalan lo que escribió el usuario
            throw new PathDomainException(e.code(), module, e);
        }
        return ModuleOptions.view(s.createModule().normalizeName(module), dryRun, result);
    }
}
