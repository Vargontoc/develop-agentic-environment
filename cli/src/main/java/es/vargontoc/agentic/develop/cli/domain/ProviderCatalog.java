package es.vargontoc.agentic.develop.cli.domain;

import java.util.List;

public interface ProviderCatalog {

    List<Provider> availableProviders();

    /** Estado del proveedor en el ámbito global, con lo que falta de los artefactos de la aplicación. */
    ProviderStatus status(ProviderType type);

    default boolean canRunApp() {
        return !availableProviders().isEmpty();
    }
}
