package es.vargontoc.agentic.develop.cli.ui.states;

import java.nio.file.Path;
import java.util.List;

import es.vargontoc.agentic.develop.cli.application.CreateModule;
import es.vargontoc.agentic.develop.cli.application.InitProject;
import es.vargontoc.agentic.develop.cli.application.ModuleResult;
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
    private final InitProject initProject;
    private final ProviderCatalog catalog;
    private final TerminalState parent;
    private String errorMessage;

    public CreateModuleState(CreateModule createModule, InitProject initProject, ProviderCatalog catalog, TerminalState parent) {
        this.createModule = createModule;
        this.initProject = initProject;
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

    // Solo los instalados y con soporte a nivel de proyecto (Codex queda fuera de momento)
    private TerminalState selectProviders(String module, List<Technician> technicians) {
        List<Provider> supported = catalog.availableProviders().stream()
            .filter(p -> p.getType().supportsProject())
            .toList();

        if(supported.isEmpty()) {
            System.out.println("[ERROR] No hay proveedores instalados compatibles con proyectos");
            return parent;
        }

        return new MultiSelectState<>(
            "Seleccione los proveedores: (Ej: 1,2)",
            supported,
            Provider::getName,
            providers -> finish(module, technicians, providers),
            parent);
    }

    private TerminalState finish(String module, List<Technician> technicians, List<Provider> providers) {
        try {
            Path project = initProject.currentProject()
                .orElseThrow(() -> new DomainException(AppErrorCode.MODULE_NO_PROJECT));
            ModuleResult result = createModule.execute(project, module, technicians, providers);
            printSummary(module, technicians, providers, result);
        } catch(DomainException e) {
            System.out.println("[ERROR] " + describe(e));
        }
        return parent;
    }

    private void printSummary(String module, List<Technician> technicians, List<Provider> providers, ModuleResult result) {
        System.out.println("Módulo '" + module + "' procesado. Técnicos: " + names(technicians)
            + ". Proveedores: " + names(providers.stream().map(Provider::getName).toList()));
        printSection("Creado", result.created());
        printSection("Omitido", result.skipped());
        printSection("Aviso", result.warnings());
    }

    private void printSection(String title, List<String> lines) {
        lines.forEach(line -> System.out.println("  [" + title + "] " + line));
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
            case MODULE_NAME_RESERVED -> "El nombre 'router' está reservado";
            case MODULE_NO_PROJECT -> "No hay un proyecto iniciado";
            case MODULE_SKILLS_NOT_FOUND -> "No se encontraron las skills del proyecto en la aplicación";
            case PROJECT_COPY_FAILED -> "No se pudieron copiar las skills al proyecto";
            case MODULE_WRITE_FAILED -> "No se pudo escribir un agente en el proyecto";
            default -> "Error: " + code.value();
        };
    }
}
