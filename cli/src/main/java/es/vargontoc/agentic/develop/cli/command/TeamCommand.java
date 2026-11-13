package es.vargontoc.agentic.develop.cli.command;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;

/** {@code team --project <ruta>}: lee módulos y opciones de {@code .sdd/team.md} de forma estructurada. */
final class TeamCommand implements Command {

    private final Supplier<Services> services;

    TeamCommand(Supplier<Services> services) {
        this.services = services;
    }

    @Override
    public String name() {
        return "team";
    }

    @Override
    public String description() {
        return "Lee los módulos y opciones de .sdd/team.md";
    }

    @Override
    public List<OptionSpec> options() {
        return List.of(ProjectOption.spec(true));
    }

    @Override
    public Object execute(ParsedArgs args) {
        Services s = services.get();
        Path project = ProjectOption.resolve(s, args);
        return TeamViews.of(s.team().read(project));
    }
}
