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
