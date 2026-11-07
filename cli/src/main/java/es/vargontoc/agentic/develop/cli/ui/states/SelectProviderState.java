package es.vargontoc.agentic.develop.cli.ui.states;

import es.vargontoc.agentic.develop.cli.application.ConfigureProvider;
import es.vargontoc.agentic.develop.cli.application.OperationResult;
import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.ui.TerminalState;

public class SelectProviderState extends MultiSelectState<Provider> {

    public SelectProviderState(ProviderCatalog catalog, ConfigureProvider configureProvider, TerminalState parent){
        super("Seleccione el proveedor: (Ej: 1,2)",
              catalog.availableProviders(),
              Provider::getName,
              chosen -> {
                  chosen.forEach(p -> configure(configureProvider, p));
                  return parent;
              },
              parent);
    }

    private static void configure(ConfigureProvider configureProvider, Provider provider) {
        System.out.println("Configurando " + provider.getType());
        print(configureProvider.execute(provider, true));
    }

    // Mismos mensajes de siempre; los avisos (recursos que faltan) antes solo iban al log
    private static void print(OperationResult result) {
        result.created().forEach(path -> System.out.println("Copied " + describe(path)));
        result.skipped().forEach(path -> System.out.println(describeExisting(path)));
        result.warnings().forEach(warning -> System.out.println("[Aviso] " + warning));
    }

    private static String describe(String path) {
        return kind(path) + " '" + name(path) + "'";
    }

    private static String describeExisting(String path) {
        return capitalize(kind(path)) + " '" + name(path) + "' exists";
    }

    private static String kind(String path) {
        if(path.startsWith("skills/"))
            return "skill";
        if(path.startsWith("hooks/"))
            return "hook";
        return path.startsWith("agents/") ? "agent" : "profile";
    }

    private static String name(String path) {
        return path.substring(path.indexOf('/') + 1);
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
