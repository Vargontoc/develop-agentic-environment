package es.vargontoc.agentic.viewer.preview;

/**
 * Resaltado ligero de sintaxis para TOML y YAML, en HTML apto para WebView.
 * No parsea los formatos: solo colorea líneas (claves, valores, comentarios y
 * secciones) para que el texto no se confunda con Markdown.
 */
public final class CodeHighlighter {

    public static final String YAML = "yaml";
    public static final String TOML = "toml";

    private static final String STYLES = """
        body { background: #151515; color: #d8d8d8; font-family: Consolas, "Cascadia Mono", monospace; font-size: 13px; margin: 10px 14px; }
        .tok-key { color: #9cdcfe; }
        .tok-string { color: #ce9178; }
        .tok-number { color: #b5cea8; }
        .tok-bool { color: #569cd6; }
        .tok-comment { color: #6a9955; }
        .tok-section { color: #c586c0; font-weight: bold; }
        .tok-punct { color: #808080; }
        """;

    private CodeHighlighter() {}

    /** Devuelve el fragmento HTML con el código resaltado; sin resaltado si el lenguaje es desconocido. */
    public static String highlight(String code, String language) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        return switch (language) {
            case TOML -> toml(code);
            case YAML -> yaml(code);
            default -> escape(code);
        };
    }

    /** Documento HTML completo con estilos oscuros, listo para {@code WebView.loadContent}. */
    public static String toDocument(String code, String language) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>"
            + STYLES + "</style></head><body><pre>"
            + highlight(code, language) + "</pre></body></html>";
    }

    private static String toml(String code) {
        StringBuilder out = new StringBuilder(code.length() + 32);
        for (String line : code.split("\\R", -1)) {
            out.append(tomlLine(line)).append('\n');
        }
        return out.toString();
    }

    private static String tomlLine(String line) {
        String indent = leadingWhitespace(line);
        String rest = line.substring(indent.length());
        if (rest.isEmpty()) {
            return escape(indent);
        }
        if (rest.startsWith("#")) {
            return escape(indent) + span("tok-comment", rest);
        }
        int close;
        if (rest.startsWith("[[") && (close = rest.indexOf("]]")) > 0) {
            return escape(indent) + span("tok-section", rest.substring(0, close + 2))
                + escape(rest.substring(close + 2));
        }
        if (rest.startsWith("[") && (close = rest.indexOf(']')) > 0) {
            return escape(indent) + span("tok-section", rest.substring(0, close + 1))
                + escape(rest.substring(close + 1));
        }
        int eq = rest.indexOf('=');
        if (eq > 0) {
            return escape(indent) + span("tok-key", rest.substring(0, eq).trim())
                + span("tok-punct", " = ") + value(rest.substring(eq + 1).trim());
        }
        return escape(line);
    }

    private static String yaml(String code) {
        StringBuilder out = new StringBuilder(code.length() + 32);
        for (String line : code.split("\\R", -1)) {
            out.append(yamlLine(line)).append('\n');
        }
        return out.toString();
    }

    private static String yamlLine(String line) {
        String indent = leadingWhitespace(line);
        String rest = line.substring(indent.length());
        if (rest.isEmpty()) {
            return escape(indent);
        }
        if (rest.startsWith("#")) {
            return escape(indent) + span("tok-comment", rest);
        }
        if (rest.equals("---") || rest.equals("...")) {
            return escape(indent) + span("tok-punct", rest);
        }
        if (rest.startsWith("-")) {
            int split = rest.startsWith("- ") ? 2 : 1;
            return escape(indent) + span("tok-punct", rest.substring(0, split))
                + yamlValue(rest.substring(split));
        }
        int colon = rest.indexOf(':');
        if (colon > 0) {
            return escape(indent) + span("tok-key", rest.substring(0, colon))
                + span("tok-punct", ":") + yamlValue(rest.substring(colon + 1));
        }
        return escape(indent) + span("tok-string", rest);
    }

    private static String yamlValue(String value) {
        String v = value.stripLeading();
        String lead = value.substring(0, value.length() - v.length());
        if (v.isEmpty()) {
            return escape(lead);
        }
        if (v.startsWith("#")) {
            return escape(lead) + span("tok-comment", v);
        }
        return escape(lead) + scalar(v);
    }

    private static String value(String v) {
        return scalar(v);
    }

    private static String scalar(String v) {
        if (v.isEmpty()) {
            return "";
        }
        if (v.startsWith("\"\"\"") || v.startsWith("'''")) {
            String triple = v.substring(0, 3);
            int end = v.indexOf(triple, 3);
            if (end >= 0) {
                return span("tok-string", v.substring(0, end + 3)) + escape(v.substring(end + 3));
            }
        }
        if (v.startsWith("\"") || v.startsWith("'")) {
            return span("tok-string", v);
        }
        if ("true".equals(v) || "false".equals(v)) {
            return span("tok-bool", v);
        }
        if (v.matches("-?\\d+(\\.\\d+)?")) {
            return span("tok-number", v);
        }
        if (v.startsWith("[")) {
            return span("tok-punct", v);
        }
        return span("tok-string", v);
    }

    private static String span(String cls, String text) {
        return "<span class=\"" + cls + "\">" + escape(text) + "</span>";
    }

    private static String leadingWhitespace(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return line.substring(0, i);
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
