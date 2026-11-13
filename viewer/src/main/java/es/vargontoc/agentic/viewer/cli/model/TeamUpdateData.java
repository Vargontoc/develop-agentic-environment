package es.vargontoc.agentic.viewer.cli.model;

import java.util.List;

/** Respuesta de {@code team-update}. */
public record TeamUpdateData(
    String path,
    boolean dryRun,
    boolean written,
    String backup,
    String hash,
    String diff,
    List<TeamData.Warning> warnings
) {

    public TeamUpdateData {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
