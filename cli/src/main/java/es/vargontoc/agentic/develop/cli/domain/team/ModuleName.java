package es.vargontoc.agentic.develop.cli.domain.team;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;

/**
 * Nombre de módulo: solo letras ASCII y guiones simples (nunca al inicio, al final
 * ni seguidos), mínimo 3 caracteres, distinto de los nombres reservados.
 */
public final class ModuleName {

    private static final int MIN_NAME_LENGTH = 3;
    private static final Set<String> RESERVED_NAMES = Set.of("router");
    private static final Pattern NAME_CHARS = Pattern.compile("[A-Za-z-]+");

    private ModuleName() {}

    /** Devuelve el nombre normalizado a minúsculas o lanza el error de validación correspondiente. */
    public static String normalize(String raw) {
        String name = raw == null ? "" : raw.trim();

        if(!NAME_CHARS.matcher(name).matches())
            throw new DomainException(AppErrorCode.MODULE_NAME_INVALID_CHARS);
        if(name.startsWith("-") || name.endsWith("-") || name.contains("--"))
            throw new DomainException(AppErrorCode.MODULE_NAME_INVALID_HYPHENS);
        if(name.length() < MIN_NAME_LENGTH)
            throw new DomainException(AppErrorCode.MODULE_NAME_TOO_SHORT);

        String normalized = name.toLowerCase(Locale.ROOT);
        if(RESERVED_NAMES.contains(normalized))
            throw new DomainException(AppErrorCode.MODULE_NAME_RESERVED);
        return normalized;
    }
}
