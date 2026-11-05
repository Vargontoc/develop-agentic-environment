package es.vargontoc.agentic.develop.cli.domain;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;

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

    /** Identificador estable (el de la carpeta de recursos), distinto del nombre visible. */
    public String id() {
        return resourceDir;
    }

    /** Lanza {@code PROVIDER_UNKNOWN} si el identificador no corresponde a ningún proveedor. */
    public static ProviderType fromId(String id) {
        for(ProviderType type : values()) {
            if(type.resourceDir.equals(id))
                return type;
        }
        throw new DomainException(AppErrorCode.PROVIDER_UNKNOWN);
    }

    /** Carpeta bajo project/agents con las cabeceras de este proveedor. */
    public String resourceDir() {
        return resourceDir;
    }

    /** Carpeta del proveedor dentro del proyecto; null si aún no se soporta a nivel de proyecto. */
    public String projectDir() {
        return projectDir;
    }

    /** Carpeta global del proveedor relativa al home del usuario, con {@code /} (la que usan los hooks de las cabeceras). */
    public String globalDirName() {
        return switch(this) {
            case CLAUDE -> ".claude";
            case CODEX -> ".codex";
            case GEMINI -> ".gemini/config";
            case OPENCODE -> ".config/opencode";
        };
    }

    public boolean supportsProject() {
        return projectDir != null;
    }

    @Override
    public String toString() {
        return name;
    }
}
