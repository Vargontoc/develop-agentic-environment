package es.vargontoc.agentic.develop.cli.domain.errors;

import java.util.Collection;
import java.util.List;

import es.vargontoc.framework.core.error.ErrorCode;
import es.vargontoc.framework.core.error.registry.ErrorCodeProvider;

public class AppErrorCodeProvider implements ErrorCodeProvider {

    @Override
    public Collection<Class<? extends ErrorCode>> errorCodeEnums() {
        return List.of(AppErrorCode.class);
    }
    
}
