package es.vargontoc.agentic.viewer.cli.model;

import java.util.List;

/**
 * Respuesta del comando `configure-provider`.
 */
public record ConfigureProviderData(
    String provider,
    String path,
    List<String> created,
    List<String> skipped,
    List<String> warnings
) {}
