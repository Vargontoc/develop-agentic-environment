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
    MODULE_NAME_INVALID_CHARS("validation.module.name_invalid_chars", ErrorCategory.VALIDATION, List.of()),
    MODULE_NAME_INVALID_HYPHENS("validation.module.name_invalid_hyphens", ErrorCategory.VALIDATION, List.of()),
    MODULE_NAME_TOO_SHORT("validation.module.name_too_short", ErrorCategory.VALIDATION, List.of()),
    SDD_RESOURCE_NOT_FOUND("internal.project.sdd_resource", ErrorCategory.INTERNAL, List.of()),
    PROJECT_COPY_FAILED("internal.project.copy", ErrorCategory.INTERNAL, List.of()),
    MODULE_NO_PROJECT("validation.module.no_project", ErrorCategory.VALIDATION, List.of()),
    MODULE_SKILLS_NOT_FOUND("internal.module.skills_resource", ErrorCategory.INTERNAL, List.of()),
    TERMINAL_COMMAND_UNKNOWN("internal.terminal.command", ErrorCategory.INTERNAL, List.of()),
    MODULE_WRITE_FAILED("internal.module.write", ErrorCategory.INTERNAL, List.of());

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
