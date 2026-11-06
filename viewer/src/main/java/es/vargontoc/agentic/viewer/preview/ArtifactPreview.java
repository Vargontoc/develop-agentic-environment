package es.vargontoc.agentic.viewer.preview;

import java.util.List;

import es.vargontoc.agentic.viewer.cli.model.DescribeData;

/**
 * Vista de un artefacto descrito: metadatos, cabecera y cuerpo con el lenguaje
 * de resaltado de cada parte, más los ficheros de una skill. Separa la cabecera
 * (frontmatter YAML o TOML) del cuerpo para no renderizarla como Markdown.
 */
public record ArtifactPreview(
    String provider,
    String scope,
    String kind,
    String name,
    String path,
    String format,
    String module,
    String status,
    boolean modifiedExternally,
    String hash,
    String headerText,
    String headerLanguage,
    String bodyText,
    String bodyLanguage,
    List<String> files
) {

    public static ArtifactPreview of(DescribeData data) {
        String headerLanguage = "";
        String bodyLanguage = "";
        if ("markdown".equals(data.format())) {
            headerLanguage = CodeHighlighter.YAML;
            bodyLanguage = "markdown";
        } else if ("codex_toml".equals(data.format())) {
            headerLanguage = CodeHighlighter.TOML;
            bodyLanguage = "markdown";
        } else if ("profile".equals(data.kind())) {
            bodyLanguage = CodeHighlighter.TOML;
        }
        return new ArtifactPreview(
            data.provider(),
            data.scope(),
            data.kind(),
            data.name(),
            data.path(),
            data.format(),
            data.module(),
            data.status(),
            data.modifiedExternally(),
            data.hash(),
            data.header(),
            headerLanguage,
            data.body(),
            bodyLanguage,
            data.files() == null ? List.of() : data.files());
    }

    public boolean hasHeader() {
        return headerText != null && !headerText.isBlank();
    }

    public boolean hasBody() {
        return bodyText != null && !bodyText.isBlank();
    }

    public boolean isSkill() {
        return "skill".equals(kind);
    }
}
