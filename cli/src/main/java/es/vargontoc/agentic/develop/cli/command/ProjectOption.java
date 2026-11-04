package es.vargontoc.agentic.develop.cli.command;

import java.nio.file.Path;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/** La opción {@code --project <ruta>} que comparten los comandos de proyecto. */
final class ProjectOption {

    static final String NAME = "project";

    private ProjectOption() {}

    static OptionSpec spec(boolean required) {
        return OptionSpec.value(NAME, required, "Ruta del proyecto");
    }

    /** @return el proyecto indicado, o null si no se indicó; los errores de ruta llevan lo que escribió el usuario */
    static Path resolve(Services services, ParsedArgs args) {
        String raw = args.optional(NAME).orElse(null);
        if(raw == null)
            return null;
        try {
            return services.paths().resolveProject(raw);
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), raw, e);
        }
    }
}
