package es.vargontoc.agentic.develop.cli.domain;

public class Provider {
    private final String name;
    private final boolean available;
    private boolean selected;

    public Provider(String name, boolean available) {
        this.name = name;
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isSelected() {
        return selected;
    }

    public String getName() {
        return name;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
