package es.vargontoc.agentic.develop.cli.domain;

import java.nio.file.Path;

public class Provider {
    private final ProviderType type;
    private final Path path;

    public Provider(ProviderType type, Path path) {
        this.type = type;
        this.path = path;
    }

    public String getName() {
        return type.toString();
    }

    public ProviderType getType() {
        return type;
    }

    public Path getPath() {
        return path;
    }
}
