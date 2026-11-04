package es.vargontoc.agentic.develop.cli.command;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Comandos disponibles, por nombre y en orden alfabético. */
public final class CommandRegistry {

    private final Map<String, Command> commands = new TreeMap<>();

    public CommandRegistry register(Command command) {
        if(commands.putIfAbsent(command.name(), command) != null)
            throw new IllegalArgumentException("Duplicate command: " + command.name());
        return this;
    }

    public Optional<Command> find(String name) {
        return Optional.ofNullable(commands.get(name));
    }

    public Collection<Command> all() {
        return commands.values();
    }
}
