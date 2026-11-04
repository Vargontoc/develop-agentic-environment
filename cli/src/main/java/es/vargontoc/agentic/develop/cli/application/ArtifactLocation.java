package es.vargontoc.agentic.develop.cli.application;

import java.nio.file.Path;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;

/**
 * Un artefacto del catálogo identificado en disco: proveedor, ámbito, carpeta del proveedor ({@code root}) y entrada.
 */
public record ArtifactLocation(ProviderType provider, ArtifactScope scope, Path root, CatalogEntry entry) {

    public Path path() {
        return root.resolve(entry.relativePath());
    }
}
