package es.vargontoc.agentic.develop.cli;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.command.CommandLine;
import es.vargontoc.agentic.develop.cli.command.CommandRegistry;
import es.vargontoc.agentic.develop.cli.command.Commands;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.ui.StateManager;
import es.vargontoc.agentic.develop.cli.ui.states.MainState;


public final class App {

    public static void main(String[] args) {

        if(args == null || args.length == 0) {
            defaultTerminal();
            return;
        }

        // Modo no interactivo: un envelope JSON por la salida de datos y el código de salida del contrato
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        System.exit(new CommandLine(commands()).run(args, out));
    }

    // Los servicios se montan solo cuando un comando los usa, y una sola vez
    static CommandRegistry commands() {
        return Commands.standard(new Supplier<>() {
            private Services services;

            @Override
            public synchronized Services get() {
                if(services == null)
                    services = Services.system();
                return services;
            }
        });
    }


    static void defaultTerminal() {

            Services services = Services.system();
            if(!services.providers().canRunApp())
                throw new DomainException(AppErrorCode.INCOMPATIBLE_ENVIRONMENT);

            try (Scanner sc = new Scanner(System.in)) {
                StateManager sm = new StateManager(new MainState(services.providers(), services.configureProvider(),
                    services.initProject(), services.createModule()));
    
                do {
                    sm.handleScanner(sc);
                }while(sm.getCurrent() != null);
            }
    }

}
