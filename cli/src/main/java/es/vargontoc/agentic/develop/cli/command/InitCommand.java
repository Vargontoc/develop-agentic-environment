package es.vargontoc.agentic.develop.cli.command;

import java.util.List;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.application.ProjectWorkspace.InitResult;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/**
 * {@code init --project <ruta>}: crea la estructura {@code .sdd} del proyecto. Si ya existe no cambia nada,
 * así que repetirlo es seguro.
 */
final class InitCommand implements Command {

    record InitView(String path, boolean created, boolean alreadyConfigured) {}

    private final Supplier<Services> services;

    InitCommand(Supplier<Services> services) {
        this.services = services;
    }

    @Override
    public String name() {
        return "init";
    }

    @Override
    public String description() {
        return "Crea la estructura .sdd de un proyecto";
    }

    @Override
    public List<OptionSpec> options() {
        return List.of(ProjectOption.spec(true));
    }

    @Override
    public Object execute(ParsedArgs args) {
        Services s = services.get();
        InitResult result;
        try {
            result = s.workspace().initialize(ProjectOption.resolve(s, args));
        } catch(PathDomainException e) {
            throw e;
        } catch(DomainException e) {
            // Los errores de la operación señalan el proyecto tal como lo escribió el usuario
            throw new PathDomainException(e.code(), args.required(ProjectOption.NAME), e);
        }
        return new InitView(Views.text(result.path()), !result.alreadyConfigured(), result.alreadyConfigured());
    }
}
