package es.vargontoc.agentic.develop.cli.infrastructure.providers;

import java.nio.file.Files;
import java.util.List;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

public class LocalProviderCatalog implements ProviderCatalog {

    private final PathResolver paths;

    public LocalProviderCatalog() {
        this(PathResolver.system());
    }

    public LocalProviderCatalog(PathResolver paths) {
        this.paths = paths;
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
}
