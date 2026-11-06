package es.vargontoc.agentic.viewer.cli.model;

/**
 * Respuesta del comando `create-artifact`.
 */
public record CreateArtifactData(
    String provider,
    String scope,
    String kind,
    String name,
    String path,
    boolean replaced,
    boolean custom,
    String backup
) {}
