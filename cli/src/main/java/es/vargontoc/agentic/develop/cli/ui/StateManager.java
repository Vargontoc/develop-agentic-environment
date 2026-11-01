package es.vargontoc.agentic.develop.cli.ui;

import java.util.Scanner;

public final class StateManager {
    
    private TerminalState current;

    public StateManager(TerminalState initial) {
        this.current = initial;
    }

    public void handleScanner(Scanner input) {
        System.out.println(current.printMenu());
        if(!input.hasNextLine()) {
            current = null;
            return;
        }
        current = current.handleChoice(input.nextLine());
    }

    public TerminalState getCurrent() {
        return current;
    }
}
