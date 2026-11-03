package es.vargontoc.agentic.develop.cli.application;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;

public class CreateModule {

    private static final Logger log = LoggerFactory.getLogger(CreateModule.class);

    private static final int MIN_NAME_LENGTH = 3;
    private static final Pattern NAME_CHARS = Pattern.compile("[A-Za-z-]+");

    /**
     * Solo letras ASCII y guiones simples (nunca al inicio, al final ni seguidos), mínimo 3 caracteres.
     * Devuelve el nombre normalizado a minúsculas.
     */
    public String normalizeName(String raw) {
        String name = raw == null ? "" : raw.trim();

        if(!NAME_CHARS.matcher(name).matches())
            throw new DomainException(AppErrorCode.MODULE_NAME_INVALID_CHARS);
        if(name.startsWith("-") || name.endsWith("-") || name.contains("--"))
            throw new DomainException(AppErrorCode.MODULE_NAME_INVALID_HYPHENS);
        if(name.length() < MIN_NAME_LENGTH)
            throw new DomainException(AppErrorCode.MODULE_NAME_TOO_SHORT);

        return name.toLowerCase(Locale.ROOT);
    }

    // Mock: la operación real sustituirá este cuerpo
    public void execute(String module, List<Technician> technicians, List<Provider> providers) {
        log.info("[MOCK] Crear módulo '{}' con técnicos {} y proveedores {}",
            module, technicians, providers.stream().map(Provider::getName).toList());
    }
}
