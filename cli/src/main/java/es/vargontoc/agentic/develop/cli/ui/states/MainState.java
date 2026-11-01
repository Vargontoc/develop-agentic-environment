package es.vargontoc.agentic.develop.cli.ui.states;

import es.vargontoc.agentic.develop.cli.infrastructure.config.AppConfiguration;
import es.vargontoc.agentic.develop.cli.ui.TerminalState;

public class MainState implements TerminalState {

    static String MENU_PRINT = """
        1. Configuración entorno
        2. Configurar proyecto
        3. Salir
        Selecciona una opción
    """;

    static String MESSAGE_CONFIG = """
        Se va a proceder a la configuración global del entorno de desarrollo. ¿Desea continuar?
    """;

    final AppConfiguration config;

    public MainState(AppConfiguration config){ this.config = config; } 

    @Override
    public String printMenu() {
        return MENU_PRINT;
    }

    @Override
    public TerminalState handleChoice(String choice) {
        if(choice.equals("3")) return null;
        if(choice.equals("1"))
            return new ConfirmationState(MESSAGE_CONFIG, new SelectProviderState(config, this),  this);

        System.out.println("Opción no válida");
        return this;
    }
    
}
