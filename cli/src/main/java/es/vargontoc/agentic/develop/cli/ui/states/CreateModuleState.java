package es.vargontoc.agentic.develop.cli.ui.states;

import java.util.List;

import es.vargontoc.agentic.develop.cli.application.CreateModule;
import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.ui.TerminalState;

/**
 * Primer paso del alta de módulo: pide el nombre y encadena la selección de técnicos y de proveedores.
 */
public class CreateModuleState implements TerminalState {

    static final String PROMPT = "Nombre del módulo (solo letras y '-', mínimo 3; vacío o 0 = cancelar):";

    private final CreateModule createModule;
    private final ProviderCatalog catalog;
    private final TerminalState parent;
    private String errorMessage;

    public CreateModuleState(CreateModule createModule, ProviderCatalog catalog, TerminalState parent) {
        this.createModule = createModule;
        this.catalog = catalog;
        this.parent = parent;
    }

    @Override
    public String printMenu() {
        if(errorMessage == null)
            return PROMPT;
        return "[ERROR] " + errorMessage + "\n" + PROMPT;
    }

    @Override
    public TerminalState handleChoice(String choice) {
        errorMessage = null;
        String input = choice.trim();

        if(input.isEmpty() || "0".equals(input))
            return parent;

        try {
            return selectTechnicians(createModule.normalizeName(input));
        } catch(DomainException e) {
            errorMessage = describe(e);
            return this;
        }
    }

    private TerminalState selectTechnicians(String module) {
        return new MultiSelectState<>(
            "Seleccione los técnicos: (Ej: 1,2)",
            List.of(Technician.values()),
            Technician::toString,
            technicians -> selectProviders(module, technicians),
            parent);
    }

    private TerminalState selectProviders(String module, List<Technician> technicians) {
        return new MultiSelectState<>(
            "Seleccione los proveedores: (Ej: 1,2)",
            catalog.availableProviders(),
            Provider::getName,
            providers -> finish(module, technicians, providers),
            parent);
    }

    private TerminalState finish(String module, List<Technician> technicians, List<Provider> providers) {
        createModule.execute(module, technicians, providers);
        System.out.println("Módulo '" + module + "' creado. Técnicos: " + names(technicians)
            + ". Proveedores: " + names(providers.stream().map(Provider::getName).toList()));
        return parent;
    }

    private String names(List<?> items) {
        return String.join(", ", items.stream().map(Object::toString).toList());
    }

    private String describe(DomainException e) {
        if(!(e.code() instanceof AppErrorCode code))
            return "Error: " + e.code().value();

        return switch(code) {
            case MODULE_NAME_INVALID_CHARS -> "El nombre solo admite letras (a-z) y guiones";
            case MODULE_NAME_INVALID_HYPHENS -> "El guion no puede ir al inicio, al final ni repetido";
            case MODULE_NAME_TOO_SHORT -> "El nombre debe tener al menos 3 caracteres";
            default -> "Error: " + code.value();
        };
    }
}
