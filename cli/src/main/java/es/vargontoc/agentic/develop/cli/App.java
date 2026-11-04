package es.vargontoc.agentic.develop.cli;

import java.util.Scanner;

import es.vargontoc.agentic.develop.cli.application.ConfigureProvider;
import es.vargontoc.agentic.develop.cli.application.CreateModule;
import es.vargontoc.agentic.develop.cli.application.InitProject;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.infrastructure.providers.LocalProviderCatalog;
import es.vargontoc.agentic.develop.cli.ui.StateManager;
import es.vargontoc.agentic.develop.cli.ui.states.MainState;


public final class App {

    public static void main(String[] args) {

        if(args == null || args.length == 0) {
            defaultTerminal();
        }else if(args.length == 2 && args[0].equals("--json") && args[1] != null && !args[1].trim().isEmpty()) {
            // handle JSON input here
            System.exit(3);
        }else {
            throw new DomainException(AppErrorCode.TERMINAL_COMMAND_UNKNOWN);
        }

        

    }


    static void defaultTerminal() {

            ProviderCatalog catalog = new LocalProviderCatalog();
            if(!catalog.canRunApp())
                throw new DomainException(AppErrorCode.INCOMPATIBLE_ENVIRONMENT);
    
            try (Scanner sc = new Scanner(System.in)) {
                StateManager sm = new StateManager(new MainState(catalog, new ConfigureProvider(), new InitProject(), new CreateModule()));
    
                do {
                    sm.handleScanner(sc);
                }while(sm.getCurrent() != null);
            }
    }

}
