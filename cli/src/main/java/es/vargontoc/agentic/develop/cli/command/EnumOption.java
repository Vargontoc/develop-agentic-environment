package es.vargontoc.agentic.develop.cli.command;

import java.util.Locale;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/** Lectura de las opciones cuyo valor es un identificador de una enumeración, en minúsculas. */
final class EnumOption {

    private EnumOption() {}

    /** @throws UsageException {@code usage.invalid_value} si el valor no corresponde a ninguna constante */
    static <E extends Enum<E>> E of(ParsedArgs args, String option, Class<E> type) {
        String raw = args.required(option);
        for(E value : type.getEnumConstants()) {
            if(id(value).equals(raw))
                return value;
        }
        throw new UsageException(UsageError.INVALID_VALUE, "--" + option + " " + raw);
    }

    /** @throws PathDomainException {@code validation.provider.unknown} con el valor escrito como ruta */
    static ProviderType provider(String raw) {
        try {
            return ProviderType.fromId(raw);
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), raw, e);
        }
    }

    static String id(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
