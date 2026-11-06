package es.vargontoc.agentic.viewer.preview;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Convierte Markdown a HTML con commonmark-java para previsualizarlo en un
 * WebView. El frontmatter no se renderiza: llega separado en la cabecera.
 */
public final class MarkdownRenderer {

    private static final String STYLES = """
        body { background: #151515; color: #d8d8d8; font-family: "Segoe UI", sans-serif; margin: 14px 18px; }
        h1, h2, h3, h4 { color: #f2f2f2; border-bottom: 1px solid #333333; padding-bottom: 4px; }
        code { background: #262626; color: #6ee7b7; padding: 1px 4px; border-radius: 4px; font-family: Consolas, monospace; }
        pre { background: #262626; padding: 10px; border-radius: 6px; overflow: auto; }
        pre code { background: transparent; color: #d8d8d8; padding: 0; }
        a { color: #93c5fd; }
        blockquote { border-left: 3px solid #3b82f6; margin-left: 0; padding-left: 12px; color: #b8b8b8; }
        table { border-collapse: collapse; }
        th, td { border: 1px solid #444444; padding: 4px 8px; }
        hr { border: none; border-top: 1px solid #333333; }
        """;

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();

    /** Convierte Markdown a un fragmento HTML. Devuelve cadena vacía para texto nulo o vacío. */
    public String toHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    /** Documento HTML completo con estilos oscuros, listo para {@code WebView.loadContent}. */
    public String toDocument(String markdown) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>"
            + STYLES + "</style></head><body>" + toHtml(markdown) + "</body></html>";
    }
}
