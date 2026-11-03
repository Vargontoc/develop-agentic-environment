package es.vargontoc.agentic.develop.cli.ui.states;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import es.vargontoc.agentic.develop.cli.ui.TerminalState;

public class MultiSelectState<T> implements TerminalState {

    static final String INVALID_SELECTION = "Selección inválida";
    static final String EMPTY_SELECTION = "Seleccione al menos una opción";

    private final String title;
    private final List<T> items;
    private final Function<T, String> label;
    private final Function<List<T>, TerminalState> onConfirm;
    private final TerminalState parent;
    private Set<Integer> selected = Set.of();
    private String errorMessage;

    public MultiSelectState(String title, List<T> items, Function<T, String> label,
                            Function<List<T>, TerminalState> onConfirm, TerminalState parent) {
        this.title = title;
        this.items = items;
        this.label = label;
        this.onConfirm = onConfirm;
        this.parent = parent;
    }

    @Override
    public String printMenu() {
        StringBuilder sb = new StringBuilder();

        if(errorMessage != null)
            sb.append("[ERROR] ").append(errorMessage).append("\n");

        sb.append(title).append("\n");

        for(int i = 0; i < items.size(); i++){
            String number = selected.contains(i) ? "[" + (i + 1) + "]" : "" + (i + 1);
            sb.append(number).append(". ").append(label.apply(items.get(i))).append("\n");
        }

        sb.append("\n\n0. Cancelar\n");
        if(!selected.isEmpty())
            sb.append("\nPulse ENTER para confirmar selección");

        return sb.toString();
    }

    @Override
    public TerminalState handleChoice(String choice) {
        errorMessage = null;
        String input = choice.trim();

        if("0".equals(input))
            return parent;

        if(input.isEmpty())
            return confirm();

        Optional<Set<Integer>> parsed = parseSelection(input);
        if(parsed.isPresent()) {
            selected = parsed.get();
        } else {
            selected = Set.of();
            errorMessage = INVALID_SELECTION;
        }
        return this;
    }

    private TerminalState confirm() {
        if(selected.isEmpty()) {
            errorMessage = EMPTY_SELECTION;
            return this;
        }

        return onConfirm.apply(selected.stream().map(items::get).toList());
    }

    private Optional<Set<Integer>> parseSelection(String choice) {
        Set<Integer> indexes = new TreeSet<>();
        for(String part : choice.split(",")) {
            if(part.isBlank()) continue;
            try {
                int index = Integer.parseInt(part.trim()) - 1;
                if(index < 0 || index >= items.size())
                    return Optional.empty();
                indexes.add(index);
            } catch(NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.of(indexes);
    }
}
