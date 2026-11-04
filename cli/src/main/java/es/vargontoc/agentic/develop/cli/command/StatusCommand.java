package es.vargontoc.agentic.develop.cli.command;

import java.util.List;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;

/** {@code status [--project <ruta>]}: estado de los proveedores y, si se indica, del proyecto. Solo lee. */
final class StatusCommand implements Command {

    private final Supplier<Services> services;

    StatusCommand(Supplier<Services> services) {
        this.services = services;
    }

    @Override
    public String name() {
        return "status";
    }

    @Override
    public String description() {
        return "Estado de los proveedores y del proyecto";
    }

    @Override
    public List<OptionSpec> options() {
        return List.of(ProjectOption.spec(false));
    }

    @Override
    public Object execute(ParsedArgs args) {
        Services s = services.get();
        return Views.of(s.status().status(ProjectOption.resolve(s, args)));
    }
}
