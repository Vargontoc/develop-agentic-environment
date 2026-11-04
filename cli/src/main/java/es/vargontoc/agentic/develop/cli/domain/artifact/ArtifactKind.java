package es.vargontoc.agentic.develop.cli.domain.artifact;

public enum ArtifactKind {
    /** Fichero de agente (Markdown o TOML). */
    AGENT,
    /** Directorio con SKILL.md y ficheros de apoyo. */
    SKILL,
    /** Fichero de configuración del proveedor (perfil de Codex). */
    PROFILE
}
