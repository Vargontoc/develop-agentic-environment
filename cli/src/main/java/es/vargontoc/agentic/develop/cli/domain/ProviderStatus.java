package es.vargontoc.agentic.develop.cli.domain;

import java.util.List;

/**
 * @param missing rutas (relativas a la carpeta del proveedor) de los artefactos de la aplicación que faltan
 */
public record ProviderStatus(ProviderType type, ProviderState state, List<String> missing) {

    public ProviderStatus {
        missing = List.copyOf(missing);
    }
}
