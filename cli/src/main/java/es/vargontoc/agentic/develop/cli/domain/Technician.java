package es.vargontoc.agentic.develop.cli.domain;

public enum Technician {
    ANALYST("Analista"), DEVELOPER("Desarrollador"), DOCUMENTER("Documentador");

    final String name;

    Technician(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
