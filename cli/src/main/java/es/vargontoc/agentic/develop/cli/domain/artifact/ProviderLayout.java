package es.vargontoc.agentic.develop.cli.domain.artifact;

import java.nio.file.Path;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;

/**
 * Dónde pone cada proveedor sus artefactos. La ruta lógica de una entrada ({@code agents/x.md}, {@code skills/x},
 * {@code profiles/x.toml}) es igual en todos los proveedores; la ubicación física depende del proveedor y del ámbito.
 * <ul>
 *   <li>Claude y OpenCode: todo bajo su carpeta ({@code .claude}, {@code .opencode}); la ubicación es la ruta lógica.</li>
 *   <li>Codex: skills en {@code .agents/skills/<n>}, agentes en {@code .codex/agents/<f>.toml} y perfiles en
 *       {@code .codex/profiles/<f>.toml}. En proyecto, el router es una skill: {@code .agents/skills/<router>/SKILL.md}.</li>
 *   <li>Gemini: global, skills en {@code .gemini/skills/<n>} y agentes en {@code .gemini/config/agents/<a>/agent.md};
 *       proyecto, {@code .agents/skills/<n>} y {@code .agents/agents/<a>/agent.md}.</li>
 * </ul>
 * La ubicación es relativa a la <b>base</b> del proveedor y ámbito: la carpeta del proveedor (Claude, OpenCode) o la
 * raíz del home / del proyecto (Codex, Gemini).
 */
public final class ProviderLayout {

    private ProviderLayout() {}

    /**
     * Base global. {@code providerHome} es la carpeta global del proveedor tal como la resuelve {@code PathResolver}
     * ({@code ~/.codex}, {@code ~/.gemini/config}), de la que se deduce el home del usuario.
     */
    public static Path globalBase(ProviderType type, Path providerHome) {
        return switch(type) {
            case CLAUDE, OPENCODE -> providerHome;
            case CODEX -> providerHome.getParent();
            case GEMINI -> providerHome.getParent().getParent();
        };
    }

    public static Path projectBase(ProviderType type, Path project) {
        return switch(type) {
            case CLAUDE -> project.resolve(".claude");
            case OPENCODE -> project.resolve(".opencode");
            case CODEX, GEMINI -> project;
        };
    }

    /** Ubicación de la entrada relativa a su base, con {@code /}. */
    public static String location(CatalogEntry entry) {
        return switch(entry.provider()) {
            case CLAUDE, OPENCODE -> entry.relativePath();
            case CODEX -> codex(entry);
            case GEMINI -> gemini(entry);
        };
    }

    /** Un router de proyecto: agente de un técnico que no pertenece a ningún módulo. */
    public static boolean isRouter(CatalogEntry entry) {
        return entry.kind() == ArtifactKind.AGENT && entry.technician() != null && entry.module() == null;
    }

    private static String codex(CatalogEntry entry) {
        if(entry.scope() == ArtifactScope.PROJECT && isRouter(entry))
            return ".agents/skills/" + entry.name() + "/SKILL.md";
        return switch(entry.kind()) {
            case SKILL -> ".agents/skills/" + entry.name();
            case AGENT -> ".codex/agents/" + fileName(entry);
            case PROFILE -> ".codex/profiles/" + fileName(entry);
        };
    }

    private static String gemini(CatalogEntry entry) {
        boolean global = entry.scope() == ArtifactScope.GLOBAL;
        return switch(entry.kind()) {
            case SKILL -> (global ? ".gemini/skills/" : ".agents/skills/") + entry.name();
            case AGENT -> (global ? ".gemini/config/agents/" : ".agents/agents/") + entry.name() + "/agent.md";
            case PROFILE -> entry.relativePath();
        };
    }

    private static String fileName(CatalogEntry entry) {
        String path = entry.relativePath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
