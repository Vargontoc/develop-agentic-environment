package es.vargontoc.agentic.develop.cli.domain;

public enum Technician {
    ANALYST("Analista", "analista"), DEVELOPER("Desarrollador", "desarrollador"), DOCUMENTER("Documentador", "documentador");

    final String name;
    private final String slug;

    Technician(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    /** Prefijo de los ficheros de agente (analista-router.md, analista-<modulo>.md…). */
    public String slug() {
        return slug;
    }

    @Override
    public String toString() {
        return name;
    }
}
