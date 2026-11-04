package es.vargontoc.agentic.develop.cli.domain.artifact;

import java.nio.file.Path;
import java.util.List;

import es.vargontoc.framework.core.error.ErrorCode;

/**
 * Resultado de inventariar un proveedor en un ámbito. Un artefacto ilegible o inválido no impide listar el resto:
 * se anota como problema con su ruta.
 */
public record Inventory(List<InstalledArtifact> artifacts, List<Problem> problems) {

    public record Problem(Path path, ErrorCode code) {}

    public Inventory {
        artifacts = List.copyOf(artifacts);
        problems = List.copyOf(problems);
    }
}
