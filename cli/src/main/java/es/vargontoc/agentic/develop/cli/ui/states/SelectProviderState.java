package es.vargontoc.agentic.develop.cli.ui.states;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import es.vargontoc.agentic.develop.cli.application.ConfigureProvider;
import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.ui.TerminalState;

public class SelectProviderState implements TerminalState {

    static final String INVALID_SELECTION = "Selección inválida";

    private final TerminalState parent;
    private final ConfigureProvider configureProvider;
    private final List<Provider> providers;
    private Set<Integer> selected = Set.of();
    private String errorMessage;

    public SelectProviderState(ProviderCatalog catalog, ConfigureProvider configureProvider, TerminalState parent){
        this.parent = parent;
        this.configureProvider = configureProvider;
        this.providers = catalog.availableProviders();
    }

    @Override
    public String printMenu() {
        StringBuilder sb = new StringBuilder();

        if(errorMessage != null)
            sb.append("[ERROR] ").append(errorMessage).append("\n");

        sb.append("Seleccione el proveedor: (Ej: 1,2)\n");

        for(int i = 0; i < providers.size(); i++){
            String number = selected.contains(i) ? "[" + (i + 1) + "]" : "" + (i + 1);
            sb.append(number).append(". ").append(providers.get(i).getName()).append("\n");
        }

        sb.append("\n\n0. Cancelar\n");
        if(!selected.isEmpty())
            sb.append("\nPulse ENTER para confirmar selección");

        return sb.toString();
    }

    @Override
    public TerminalState handleChoice(String choice) {
        errorMessage = null;

        if("0".equals(choice))
            return parent;

        if(choice.isEmpty())
            return confirm();

        Optional<Set<Integer>> parsed = parseSelection(choice);
        if(parsed.isPresent()) {
            selected = parsed.get();
        } else {
            selected = Set.of();
            errorMessage = INVALID_SELECTION;
        }
        return this;
    }

    private TerminalState confirm() {
        if(selected.isEmpty())
            return this;

        selected.forEach(i -> configureProvider.execute(providers.get(i)));
        return parent;
    }

    private Optional<Set<Integer>> parseSelection(String choice) {
        Set<Integer> indexes = new TreeSet<>();
        for(String part : choice.split(",")) {
            if(part.isBlank()) continue;
            try {
                int index = Integer.parseInt(part.trim()) - 1;
                if(index < 0 || index >= providers.size())
                    return Optional.empty();
                indexes.add(index);
            } catch(NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.of(indexes);
    }
}
