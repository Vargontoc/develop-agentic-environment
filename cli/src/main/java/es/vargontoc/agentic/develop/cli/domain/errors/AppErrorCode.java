package es.vargontoc.agentic.develop.cli.domain.errors;

import java.util.List;

import es.vargontoc.framework.core.error.ErrorCategory;
import es.vargontoc.framework.core.error.ErrorCode;
import es.vargontoc.framework.core.error.parameter.ErrorParameter;

public enum AppErrorCode implements ErrorCode {
    
    INCOMPATIBLE_ENVIRONMENT("internal.env", ErrorCategory.INTERNAL, List.of());

    private final String value;
    private final ErrorCategory category;
    private final List<ErrorParameter> params;

    AppErrorCode(String value, ErrorCategory category, List<ErrorParameter> params) {
        this.value = value;
        this.category = category;
        this.params = params;
    }

    @Override
    public ErrorCategory category() { return category; }
    @Override
    public List<ErrorParameter> parameters() { return params; }
    @Override
    public String value() { return value; }

}
