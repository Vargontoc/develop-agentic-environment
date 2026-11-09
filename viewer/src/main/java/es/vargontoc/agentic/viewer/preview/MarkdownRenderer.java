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
        body { background: #12151a; color: #dfe3e8; font-family: "Segoe UI", sans-serif; font-size: 14px; line-height: 1.55; margin: 18px 22px; }
        h1, h2, h3, h4 { color: #f3f5f7; border-bottom: 1px solid #2e343d; padding-bottom: 4px; }
        code { background: #1f242b; color: #8ee6b0; padding: 1px 5px; border-radius: 4px; font-family: Consolas, "Cascadia Mono", monospace; }
        pre { background: #1a1e24; padding: 12px; border-radius: 8px; border: 1px solid #2e343d; overflow: auto; }
        pre code { background: transparent; color: #dfe3e8; padding: 0; }
        a { color: #8ab4ff; }
        blockquote { border-left: 3px solid #4f8cff; margin-left: 0; padding-left: 14px; color: #b9c0c9; }
        table { border-collapse: collapse; }
        th, td { border: 1px solid #39414b; padding: 6px 10px; }
        th { background: #1c2128; }
        hr { border: none; border-top: 1px solid #2e343d; }
        ::selection { background: #2f4f85; }
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
