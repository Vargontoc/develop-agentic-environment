package es.vargontoc.agentic.develop.cli.domain;

public enum ProviderType {
    CLAUDE("Claude", "claude", ".claude"),
    CODEX("Codex", "codex", null),
    GEMINI("Gemini", "gemini", ".agents"),
    OPENCODE("OpenCode", "opencode", ".opencode");

    final String name;
    private final String resourceDir;
    private final String projectDir;

    ProviderType(String name, String resourceDir, String projectDir) {
        this.name = name;
        this.resourceDir = resourceDir;
        this.projectDir = projectDir;
    }

    /** Carpeta bajo project/agents con las cabeceras de este proveedor. */
    public String resourceDir() {
        return resourceDir;
    }

    /** Carpeta del proveedor dentro del proyecto; null si aún no se soporta a nivel de proyecto. */
    public String projectDir() {
        return projectDir;
    }

    public boolean supportsProject() {
        return projectDir != null;
    }

    @Override
    public String toString() {
        return name;
    }
}
