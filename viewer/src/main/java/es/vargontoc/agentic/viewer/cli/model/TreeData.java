package es.vargontoc.agentic.viewer.cli.model;

import java.util.List;

public record TreeData(List<Node> global, List<Node> project, List<Warning> warnings, List<Problem> problems) {

    public record Node(String type, String name, String path, String provider, String status,
                       boolean pending, List<Node> children) {
    }

    public record Warning(String type, int line) {
    }

    public record Problem(String path, String code) {
    }
}
