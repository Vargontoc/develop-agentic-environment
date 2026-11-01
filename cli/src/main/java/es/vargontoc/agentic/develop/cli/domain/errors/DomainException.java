package es.vargontoc.agentic.develop.cli.domain.errors;

import es.vargontoc.framework.core.error.ErrorCode;
import es.vargontoc.framework.core.error.ModelException;

public class DomainException extends ModelException {

    public DomainException(ErrorCode code) {
        super(code);
    }
    
}
