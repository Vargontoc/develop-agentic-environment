package es.vargontoc.agentic.viewer.cli.model;

import java.util.List;

/**
 * Respuesta del comando `catalog`.
 */
public record CatalogData(
    String provider,
    String scope,
    String kind,
    List<CatalogEntry> entries
) {
    public record CatalogEntry(
        String name,
        String relativePath,
        String path,
        String module,
        String technician,
        boolean exists,
        Template template
    ) {}

    public record Template(
        String header,
        String body,
        String content,
        List<TemplateFile> files
    ) {}

    public record TemplateFile(
        String path,
        String content
    ) {}
}
