package es.vargontoc.agentic.develop.cli.ui.states;

import es.vargontoc.agentic.develop.cli.application.ConfigureProvider;
import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.ui.TerminalState;

public class SelectProviderState extends MultiSelectState<Provider> {

    public SelectProviderState(ProviderCatalog catalog, ConfigureProvider configureProvider, TerminalState parent){
        super("Seleccione el proveedor: (Ej: 1,2)",
              catalog.availableProviders(),
              Provider::getName,
              chosen -> {
                  chosen.forEach(p -> configureProvider.execute(p, true));
                  return parent;
              },
              parent);
    }
}
