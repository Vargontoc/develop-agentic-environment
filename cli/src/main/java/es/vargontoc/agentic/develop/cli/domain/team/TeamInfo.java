package es.vargontoc.agentic.develop.cli.domain.team;

import java.util.List;

public record TeamInfo(List<ModuleEntry> modules, List<TeamWarning> warnings) {

    public TeamInfo {
        modules = List.copyOf(modules);
        warnings = List.copyOf(warnings);
    }

    public static TeamInfo withWarning(TeamWarning warning) {
        return new TeamInfo(List.of(), List.of(warning));
    }

    public List<String> moduleNames() {
        return modules.stream().map(ModuleEntry::name).toList();
    }
}
