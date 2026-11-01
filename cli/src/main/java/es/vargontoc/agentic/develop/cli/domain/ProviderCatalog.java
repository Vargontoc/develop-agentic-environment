package es.vargontoc.agentic.develop.cli.domain;

import java.util.List;

public interface ProviderCatalog {

    List<Provider> availableProviders();

    default boolean canRunApp() {
        return !availableProviders().isEmpty();
    }
}
