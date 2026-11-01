package es.vargontoc.agentic.develop.cli;

import java.util.Scanner;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.states.MainState;
import es.vargontoc.agentic.develop.cli.domain.states.StateManager;
import es.vargontoc.agentic.develop.cli.domain.validators.EnvironmentValidator;


public final class App {

    public static void main(String[] args) {

        if(!EnvironmentValidator.canRunApp())
            throw new DomainException(AppErrorCode.INCOMPATIBLE_ENVIRONMENT);

        try (Scanner sc = new Scanner(System.in)) {
            StateManager sm = new StateManager(new MainState());
            
            do {
                sm.handleScanner(sc);
            }while(sm.getCurrent() != null);
        }
    }

}
