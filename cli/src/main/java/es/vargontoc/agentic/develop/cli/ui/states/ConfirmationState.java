package es.vargontoc.agentic.develop.cli.ui.states;

import es.vargontoc.agentic.develop.cli.ui.TerminalState;

public class ConfirmationState implements TerminalState {

    private final String msg;
    private final TerminalState next;
    private final TerminalState cancel;

    public ConfirmationState(String msg, TerminalState next, TerminalState cancel) { this.msg = msg; this.next = next; this.cancel = cancel;     }


    @Override
    public String printMenu() {
        return msg + " (s/n)";
    }

    @Override
    public TerminalState handleChoice(String choice) {
        if(choice.equals("s") || choice.equals("S")) return next;
        return cancel;
    }
}
