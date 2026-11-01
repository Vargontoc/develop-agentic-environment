package es.vargontoc.agentic.develop.cli.domain;

public class Provider {
    private final ProviderType type;

    public Provider(ProviderType type) {
        this.type = type;
    }

    public String getName() {
        return type.toString();
    }

    public ProviderType getType() {
        return type;
    }

}
