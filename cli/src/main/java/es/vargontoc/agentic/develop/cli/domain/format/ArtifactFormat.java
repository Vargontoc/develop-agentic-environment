package es.vargontoc.agentic.develop.cli.domain.format;

/**
 * Formatos de agente soportados. Separan y componen por texto: la cabecera nunca se interpreta ni se reconstruye.
 */
public enum ArtifactFormat {
    /** Frontmatter entre líneas "---" seguido del cuerpo. */
    MARKDOWN,
    /** TOML de Codex: el cuerpo es la cadena multilínea de {@code developer_instructions}. */
    CODEX_TOML;

    /** Separa un artefacto instalado. Lanza {@code ARTIFACT_FORMAT_INVALID} si no se reconoce la cabecera. */
    public ArtifactParts split(String text) {
        return switch(this) {
            case MARKDOWN -> MarkdownCodec.split(text);
            case CODEX_TOML -> CodexTomlCodec.split(text);
        };
    }

    /** Recompone un artefacto; {@code compose(split(t))} devuelve {@code t} salvo escapes TOML equivalentes. */
    public String compose(ArtifactParts parts) {
        return switch(this) {
            case MARKDOWN -> MarkdownCodec.compose(parts);
            case CODEX_TOML -> CodexTomlCodec.compose(parts);
        };
    }

    /**
     * Genera un artefacto nuevo a partir del fichero de cabecera de un recurso y un cuerpo.
     * En TOML la cabecera del recurso termina en {@code developer_instructions = } y aún no lleva comillas.
     */
    public String assemble(String headerResource, String body) {
        return switch(this) {
            case MARKDOWN -> headerResource.stripTrailing() + "\n\n" + body;
            case CODEX_TOML -> compose(new ArtifactParts(headerResource + CodexTomlCodec.DELIMITER, body));
        };
    }
}
