package es.vargontoc.agentic.develop.cli.domain;

public class Provider {
    private final ProviderType type;
    private final boolean available;
    private boolean selected;

    public Provider(ProviderType type, boolean available) {
        this.type = type;
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isSelected() {
        return selected;
    }

    public String getName() {
        return type.toString();
    }

    public ProviderType getType() {
        return type;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
