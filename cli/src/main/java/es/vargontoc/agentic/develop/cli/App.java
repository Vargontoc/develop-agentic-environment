package es.vargontoc.agentic.develop.cli;

import java.util.Scanner;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.infrastructure.config.AppConfiguration;
import es.vargontoc.agentic.develop.cli.ui.StateManager;
import es.vargontoc.agentic.develop.cli.ui.states.MainState;


public final class App {

    public static void main(String[] args) {

        AppConfiguration config = new AppConfiguration();
        if(!config.canRunApp())
            throw new DomainException(AppErrorCode.INCOMPATIBLE_ENVIRONMENT);

        try (Scanner sc = new Scanner(System.in)) {
            StateManager sm = new StateManager(new MainState(config));
            
            do {
                sm.handleScanner(sc);
            }while(sm.getCurrent() != null);
        }
    }

}
