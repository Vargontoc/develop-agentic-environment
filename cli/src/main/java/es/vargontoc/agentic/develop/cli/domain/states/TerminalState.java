package es.vargontoc.agentic.develop.cli.domain.states;

public interface TerminalState {
    
    String printMenu();
    TerminalState handleChoice(String choice);


}
