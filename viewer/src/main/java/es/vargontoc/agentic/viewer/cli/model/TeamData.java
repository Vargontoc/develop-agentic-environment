package es.vargontoc.agentic.viewer.cli.model;

import java.util.List;

/** Respuesta de {@code team}: módulos y opciones de .sdd/team.md. */
public record TeamData(
    String path,
    String hash,
    List<Module> modules,
    Options options,
    List<Warning> warnings
) {

    public TeamData {
        modules = modules == null ? List.of() : List.copyOf(modules);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        options = options == null ? new Options("si", "balanced", "implementation-first", null) : options;
    }

    public record Module(String name, String mapId, List<String> paths) {
        public Module {
            paths = paths == null ? List.of() : List.copyOf(paths);
        }
    }

    public record Options(String sprints, String gitPolicy, String contractStrategy, String readmeOwner) {}

    public record Warning(String type, int line) {}
}
