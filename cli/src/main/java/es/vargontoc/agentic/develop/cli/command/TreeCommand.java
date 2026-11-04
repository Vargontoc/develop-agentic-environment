package es.vargontoc.agentic.develop.cli.command;

import java.util.List;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;

/** {@code tree [--project <ruta>]}: árbol global y, si se indica, de proyecto. Solo lee. */
final class TreeCommand implements Command {

    private final Supplier<Services> services;

    TreeCommand(Supplier<Services> services) {
        this.services = services;
    }

    @Override
    public String name() {
        return "tree";
    }

    @Override
    public String description() {
        return "Árbol de proveedores, agentes, skills, módulos y técnicos";
    }

    @Override
    public List<OptionSpec> options() {
        return List.of(ProjectOption.spec(false));
    }

    @Override
    public Object execute(ParsedArgs args) {
        Services s = services.get();
        return Views.of(s.tree().build(ProjectOption.resolve(s, args)));
    }
}
