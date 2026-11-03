package es.vargontoc.agentic.develop.cli.domain.errors;

import java.util.Map;

import es.vargontoc.framework.core.error.ErrorCode;
import es.vargontoc.framework.core.error.ModelException;

public class DomainException extends ModelException {

    public DomainException(ErrorCode code) {
        super(code);
    }

    public DomainException(ErrorCode code, Throwable cause) {
        super(code, Map.of(), cause);
    }

}
