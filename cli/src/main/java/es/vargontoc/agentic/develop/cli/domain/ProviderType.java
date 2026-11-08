package es.vargontoc.agentic.develop.cli.domain;

import java.util.List;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;

public enum ProviderType {
    CLAUDE("Claude", "claude", ".claude"),
    CODEX("Codex", "codex", ".agents", ".codex"),
    GEMINI("Gemini", "gemini", ".agents"),
    OPENCODE("OpenCode", "opencode", ".opencode");

    final String name;
    private final String resourceDir;
    private final List<String> projectDirs;

    ProviderType(String name, String resourceDir, String... projectDirs) {
        this.name = name;
        this.resourceDir = resourceDir;
        this.projectDirs = List.of(projectDirs);
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

    /**
     * Carpetas del proveedor dentro del proyecto, relativas a su raíz. Un proveedor puede repartir sus artefactos en
     * varias (Codex: skills en {@code .agents}, agentes y perfiles en {@code .codex}) y varios proveedores pueden
     * compartir una (Codex y Gemini: {@code .agents/skills}).
     */
    public List<String> projectDirs() {
        return projectDirs;
    }

    /** Carpeta de agentes del proveedor en el proyecto, relativa a su raíz (la que sustituye a {@code {slug-path-agents}}). */
    public String projectAgentsDir() {
        return switch(this) {
            case CLAUDE -> ".claude/agents";
            case CODEX -> ".codex/agents";
            case GEMINI -> ".agents/agents";
            case OPENCODE -> ".opencode/agents";
        };
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

    @Override
    public String toString() {
        return name;
    }
}
