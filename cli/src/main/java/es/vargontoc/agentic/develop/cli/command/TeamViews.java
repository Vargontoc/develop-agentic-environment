package es.vargontoc.agentic.develop.cli.command;

import java.util.List;

import es.vargontoc.agentic.develop.cli.application.TeamService;
import es.vargontoc.agentic.develop.cli.domain.team.TeamOptions;
import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning;

/** Formas JSON de {@code team} y {@code team-update}. */
final class TeamViews {

    private TeamViews() {}

    record ModuleView(String name, String mapId, List<String> paths) {}

    record OptionsView(String sprints, String gitPolicy, String contractStrategy, String readmeOwner) {}

    record WarningView(String type, int line) {}

    record TeamResponse(String path, String hash, List<ModuleView> modules, OptionsView options,
                        List<WarningView> warnings) {}

    record UpdateResponse(String path, boolean dryRun, boolean written, String backup, String hash,
                          String diff, List<WarningView> warnings) {}

    record UpdateRequest(OptionsView options, List<ModuleView> modules) {}

    static TeamResponse of(TeamService.TeamView view) {
        return new TeamResponse(Views.text(view.path()), view.hash(), moduleViews(view.modules()),
            options(view.options()), warnings(view.warnings()));
    }

    static UpdateResponse of(TeamService.UpdateResult result) {
        return new UpdateResponse(Views.text(result.path()), result.dryRun(), result.written(),
            result.backup() == null ? null : Views.text(result.backup()), result.hash(), result.diff(),
            warnings(result.warnings()));
    }

    static TeamOptions options(OptionsView view) {
        if(view == null)
            return TeamOptions.DEFAULTS;
        return new TeamOptions(view.sprints(), view.gitPolicy(), view.contractStrategy(), view.readmeOwner());
    }

    static List<TeamService.ModuleSpec> moduleSpecs(List<ModuleView> views) {
        if(views == null)
            return List.of();
        return views.stream()
            .map(v -> new TeamService.ModuleSpec(v.name(), v.mapId(), v.paths()))
            .toList();
    }

    private static List<ModuleView> moduleViews(List<TeamService.ModuleSpec> modules) {
        return modules.stream()
            .map(m -> new ModuleView(m.name(), m.mapId(), m.paths()))
            .toList();
    }

    private static OptionsView options(TeamOptions options) {
        return new OptionsView(options.sprints(), options.gitPolicy(), options.contractStrategy(),
            options.readmeOwner());
    }

    private static List<WarningView> warnings(List<TeamWarning> warnings) {
        return warnings.stream()
            .map(w -> new WarningView(w.type().name().toLowerCase(java.util.Locale.ROOT), w.line()))
            .toList();
    }
}
