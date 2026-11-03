package es.vargontoc.agentic.develop.cli.ui.states;

import es.vargontoc.agentic.develop.cli.application.InitProject;
import es.vargontoc.agentic.develop.cli.application.ProjectStart;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.ui.TerminalState;

public class InitProjectState implements TerminalState {

    static final String PROMPT = "Ruta del proyecto (vacío = directorio actual, 0 = cancelar):";

    private final InitProject initProject;
    private final TerminalState onSuccess;
    private final TerminalState onCancel;
    private String errorMessage;

    public InitProjectState(InitProject initProject, TerminalState parent) {
        this(initProject, parent, parent);
    }

    public InitProjectState(InitProject initProject, TerminalState onSuccess, TerminalState onCancel) {
        this.initProject = initProject;
        this.onSuccess = onSuccess;
        this.onCancel = onCancel;
    }

    @Override
    public String printMenu() {
        if(errorMessage == null)
            return PROMPT;
        return "[ERROR] " + errorMessage + "\n" + PROMPT;
    }

    @Override
    public TerminalState handleChoice(String choice) {
        errorMessage = null;

        if("0".equals(choice.trim()))
            return onCancel;

        try {
            ProjectStart started = initProject.start(choice);
            if(started.alreadyConfigured())
                System.out.println("Proyecto ya configurado, seleccionado: " + started.path());
            else
                System.out.println("Proyecto iniciado en " + started.path());
            return onSuccess;
        } catch(DomainException e) {
            errorMessage = describe(e, choice.trim());
            return this;
        }
    }

    private String describe(DomainException e, String input) {
        String target = input.isEmpty() ? "el directorio actual" : input;

        if(!(e.code() instanceof AppErrorCode code))
            return "Error: " + e.code().value();

        return switch(code) {
            case PROJECT_PATH_INVALID -> "Ruta no válida: " + target;
            case PROJECT_PATH_NOT_FOUND -> "La ruta no existe: " + target;
            case PROJECT_PATH_NOT_DIRECTORY -> "La ruta no es un directorio: " + target;
            case SDD_RESOURCE_NOT_FOUND -> "No se encontró el recurso .sdd en la aplicación";
            case PROJECT_COPY_FAILED -> "No se pudo copiar el .sdd al proyecto";
            default -> "Error: " + code.value();
        };
    }
}
