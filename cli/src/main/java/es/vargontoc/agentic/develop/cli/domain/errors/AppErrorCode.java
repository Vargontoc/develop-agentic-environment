package es.vargontoc.agentic.develop.cli.domain.errors;

import java.util.List;

import es.vargontoc.framework.core.error.ErrorCategory;
import es.vargontoc.framework.core.error.ErrorCode;
import es.vargontoc.framework.core.error.parameter.ErrorParameter;

public enum AppErrorCode implements ErrorCode {
    
    INCOMPATIBLE_ENVIRONMENT("internal.env", ErrorCategory.INTERNAL, List.of()),

    PROJECT_PATH_INVALID("validation.project.path_invalid", ErrorCategory.VALIDATION, List.of()),
    PROJECT_PATH_NOT_FOUND("not_found.project.path", ErrorCategory.NOT_FOUND, List.of()),
    PROJECT_PATH_NOT_DIRECTORY("validation.project.not_directory", ErrorCategory.VALIDATION, List.of()),
    PROJECT_ALREADY_CONFIGURED("conflict.project.configured", ErrorCategory.CONFLICT, List.of()),
    SDD_RESOURCE_NOT_FOUND("internal.project.sdd_resource", ErrorCategory.INTERNAL, List.of()),
    PROJECT_COPY_FAILED("internal.project.copy", ErrorCategory.INTERNAL, List.of());

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
