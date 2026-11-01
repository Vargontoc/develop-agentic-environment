package es.vargontoc.agentic.develop.cli.ui;

public interface TerminalState {
    
    String printMenu();
    TerminalState handleChoice(String choice);


}
