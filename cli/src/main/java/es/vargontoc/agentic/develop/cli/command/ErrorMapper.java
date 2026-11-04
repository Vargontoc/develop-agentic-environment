package es.vargontoc.agentic.develop.cli.command;

import es.vargontoc.agentic.develop.cli.command.Envelope.ErrorEntry;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.framework.core.error.ErrorCode;
import es.vargontoc.framework.core.error.ModelException;

/**
 * Traduce cualquier fallo al error del envelope y al código de salida:
 * uso incorrecto → 2; error esperado del dominio (validación, no encontrado…) → 1; error interno o inesperado → 3.
 * Nunca incluye trazas: eso va al log por la salida de error.
 */
final class ErrorMapper {

    static final String UNEXPECTED_CODE = "internal.unexpected";

    record Mapped(ExitCode exit, ErrorEntry error) {}

    private ErrorMapper() {}

    static Mapped map(Throwable failure) {
        if(failure instanceof UsageException usage)
            return new Mapped(ExitCode.USAGE_ERROR, new ErrorEntry(usage.error().value(), usage.detail(), usage.getMessage()));

        if(failure instanceof ModelException model) {
            ErrorCode code = model.code();
            String path = model instanceof PathDomainException located ? located.location() : null;
            ExitCode exit = code.category().expected() ? ExitCode.DOMAIN_ERROR : ExitCode.INTERNAL_ERROR;
            return new Mapped(exit, new ErrorEntry(code.value(), path, messageOf(code)));
        }

        return new Mapped(ExitCode.INTERNAL_ERROR, new ErrorEntry(UNEXPECTED_CODE, null, "Error interno inesperado"));
    }

    private static String messageOf(ErrorCode code) {
        return code instanceof AppErrorCode app ? ErrorMessages.of(app) : "Error: " + code.value();
    }
}
