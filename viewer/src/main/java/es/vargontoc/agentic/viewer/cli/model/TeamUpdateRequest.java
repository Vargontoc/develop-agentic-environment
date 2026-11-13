package es.vargontoc.agentic.viewer.cli.model;

import java.util.List;

/** Estado completo que se envía a {@code team-update --stdin}. */
public record TeamUpdateRequest(TeamData.Options options, List<TeamData.Module> modules) {

    public TeamUpdateRequest {
        modules = modules == null ? List.of() : List.copyOf(modules);
        options = options == null
            ? new TeamData.Options("si", "balanced", "implementation-first", null)
            : options;
    }
}
