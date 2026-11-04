package es.vargontoc.agentic.develop.cli.infrastructure.providers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.domain.ProviderState;
import es.vargontoc.agentic.develop.cli.domain.ProviderStatus;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactCatalog;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

public class LocalProviderCatalog implements ProviderCatalog {

    private final PathResolver paths;
    private final ArtifactCatalog artifacts;

    public LocalProviderCatalog() {
        this(PathResolver.system(), new ResourceArtifactCatalog());
    }

    public LocalProviderCatalog(PathResolver paths, ArtifactCatalog artifacts) {
        this.paths = paths;
        this.artifacts = artifacts;
    }

    @Override
    public List<Provider> availableProviders() {
        if(!paths.hasHome())
            return List.of();

        return List.of(ProviderType.values()).stream()
            .map(t -> new Provider(t, paths.providerHome(t)))
            .filter(p -> Files.exists(p.getPath()))
            .toList();
    }

    /**
     * Solo comprueba la existencia de las rutas esperadas del catálogo: nunca recorre la carpeta del proveedor,
     * así que los artefactos ajenos no intervienen.
     */
    @Override
    public ProviderStatus status(ProviderType type) {
        if(!paths.hasHome() || !Files.exists(paths.providerHome(type)))
            return new ProviderStatus(type, ProviderState.NOT_INSTALLED, List.of());

        Path home = paths.providerHome(type);
        List<CatalogEntry> expected = artifacts.entries(type, ArtifactScope.GLOBAL);
        List<String> missing = expected.stream()
            .map(CatalogEntry::relativePath)
            .filter(path -> !Files.exists(home.resolve(path)))
            .toList();

        if(missing.isEmpty())
            return new ProviderStatus(type, ProviderState.CONFIGURED, missing);
        if(missing.size() == expected.size())
            return new ProviderStatus(type, ProviderState.UNCONFIGURED, missing);
        return new ProviderStatus(type, ProviderState.PARTIAL, missing);
    }
}
