package es.vargontoc.agentic.viewer.cli.model;

import java.util.List;

public record StatusData(List<Provider> providers, Project project) {

    public record Provider(String id, String name, String state, List<String> missing) {
    }

    public record Project(String path, boolean initialized, List<String> modules, List<Warning> warnings) {
    }

    public record Warning(String type, int line) {
    }
}
