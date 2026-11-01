package es.vargontoc.agentic.develop.cli.domain.states;

import java.util.Scanner;

public final class StateManager {
    
    private TerminalState current;

    public StateManager(TerminalState initial) {
        this.current = initial;
    }

    public void handleScanner(Scanner input) {
        System.out.println(current.printMenu());
        current = current.handleChoice(input.nextLine());
    }

    public TerminalState getCurrent() {
        return current;
    }
}
