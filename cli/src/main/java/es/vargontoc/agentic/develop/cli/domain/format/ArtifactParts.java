package es.vargontoc.agentic.develop.cli.domain.format;

import java.util.Objects;

/**
 * Un artefacto separado en tres trozos de texto: cabecera, cuerpo y cola.
 * La cabecera y la cola se conservan tal cual (no se reserializan); el cuerpo es el texto lógico, sin escapes del formato.
 * La cola solo la usa TOML (lo que sigue a las comillas de cierre).
 */
public record ArtifactParts(String header, String body, String suffix) {

    public ArtifactParts {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(suffix, "suffix");
    }

    public ArtifactParts(String header, String body) {
        this(header, body, "");
    }

    public ArtifactParts withBody(String newBody) {
        return new ArtifactParts(header, newBody, suffix);
    }
}
