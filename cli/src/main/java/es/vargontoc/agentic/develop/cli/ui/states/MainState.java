package es.vargontoc.agentic.develop.cli.ui.states;

import es.vargontoc.agentic.develop.cli.application.ConfigureProvider;
import es.vargontoc.agentic.develop.cli.application.CreateModule;
import es.vargontoc.agentic.develop.cli.application.InitProject;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.ui.TerminalState;

public class MainState implements TerminalState {

    static String MENU_PRINT = """
        1. Configuración entorno
        2. Iniciar proyecto 
        3. Crear módulo
        4. Salir
        Selecciona una opción
    """;

    static String MESSAGE_CONFIG = """
        Se va a proceder a la configuración global del entorno de desarrollo. ¿Desea continuar?
    """;

    private final ProviderCatalog catalog;
    private final ConfigureProvider configureProvider;
    private final InitProject initProject;
    private final CreateModule createModule;
    private String errorMessage;

    public MainState(ProviderCatalog catalog, ConfigureProvider configureProvider, InitProject initProject, CreateModule createModule){
        this.catalog = catalog;
        this.configureProvider = configureProvider;
        this.initProject = initProject;
        this.createModule = createModule;
    }

    @Override
    public String printMenu() {
        if(errorMessage == null)
            return MENU_PRINT;
        return "[ERROR] " + errorMessage + "\n" + MENU_PRINT;
    }

    @Override
    public TerminalState handleChoice(String choice) {
        errorMessage = null;
        if(choice.equals("4")) return null;
        if(choice.equals("1"))
            return new ConfirmationState(MESSAGE_CONFIG, new SelectProviderState(catalog, configureProvider, this),  this);
        if(choice.equals("2"))
            return startProject();
        if(choice.equals("3"))
            return newModule();

        errorMessage = "Opción no válida";
        return this;
    }

    // Sin proyecto en memoria se ofrece iniciarlo y, al terminar, se continúa con el módulo
    private TerminalState newModule() {
        TerminalState module = new CreateModuleState(createModule, initProject, catalog, this);

        if(initProject.currentProject().isPresent())
            return module;

        return new ConfirmationState(
            "No hay un proyecto iniciado. ¿Desea iniciarlo ahora?",
            new InitProjectState(initProject, module, this),
            this);
    }

    private TerminalState startProject() {
        TerminalState init = new InitProjectState(initProject, this);

        return initProject.currentProject()
            .<TerminalState>map(path -> new ConfirmationState(
                "Ya hay un proyecto iniciado en " + path + ". ¿Desea iniciar otro?", init, this))
            .orElse(init);
    }

}
