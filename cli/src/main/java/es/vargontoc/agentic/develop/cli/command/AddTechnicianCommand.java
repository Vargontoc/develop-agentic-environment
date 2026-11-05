package es.vargontoc.agentic.develop.cli.command;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.application.OperationResult;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/**
 * {@code add-technician --project <ruta> --module <nombre> --technicians <lista> --providers <lista> [--dry-run]}:
 * genera solo los agentes de esos técnicos en un módulo ya registrado. Los técnicos previos no se tocan y lo que ya
 * existe se omite. Con {@code --dry-run} devuelve lo previsto sin escribir.
 */
final class AddTechnicianCommand implements Command {

    private final Supplier<Services> services;

    AddTechnicianCommand(Supplier<Services> services) {
        this.services = services;
    }

    @Override
    public String name() {
        return "add-technician";
    }

    @Override
    public String description() {
        return "Añade técnicos a un módulo existente";
    }

    @Override
    public List<OptionSpec> options() {
        return ModuleOptions.specs("Nombre del módulo existente");
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
            result = s.createModule().addTechnicians(project, module, technicians, providers, dryRun);
        } catch(PathDomainException e) {
            throw e;
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), module, e);
        }
        return ModuleOptions.view(s.createModule().normalizeName(module), dryRun, result);
    }
}
