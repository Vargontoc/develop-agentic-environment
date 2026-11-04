package es.vargontoc.agentic.develop.cli.command;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Opciones ya validadas de un comando. */
public record ParsedArgs(Map<String, String> values, Set<String> flags) {

    public ParsedArgs {
        values = Map.copyOf(values);
        flags = Set.copyOf(flags);
    }

    /** Valor de una opción que el comando declaró obligatoria. */
    public String required(String name) {
        return values.get(name);
    }

    public Optional<String> optional(String name) {
        return Optional.ofNullable(values.get(name));
    }

    public boolean flag(String name) {
        return flags.contains(name);
    }
}
