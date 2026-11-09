package es.vargontoc.agentic.viewer.editor;

/**
 * Presentación del cuerpo compartido de un especialista: resalta el placeholder
 * {@code {module}} y permite alternar su versión resuelta. Puro y sin JavaFX.
 */
public final class SharedBody {

    public static final String MODULE_PLACEHOLDER = "{module}";

    private static final String STYLES = """
        body { background: #12151a; color: #dfe3e8; font-family: Consolas, "Cascadia Mono", monospace; font-size: 13px; line-height: 1.5; margin: 14px 18px; }
        .placeholder { color: #ffd479; background: #3a2f14; border-radius: 3px; padding: 0 3px; }
        """;

    private SharedBody() {}

    public static boolean hasPlaceholder(String sharedBody) {
        return sharedBody != null && sharedBody.contains(MODULE_PLACEHOLDER);
    }

    public static String resolve(String sharedBody, String module) {
        if (sharedBody == null) {
            return "";
        }
        if (module == null || module.isBlank()) {
            return sharedBody;
        }
        return sharedBody.replace(MODULE_PLACEHOLDER, module);
    }

    /** Fragmento HTML con el placeholder resaltado, o su versión resuelta si {@code resolved} es true. */
    public static String toHtml(String sharedBody, String module, boolean resolved) {
        if (sharedBody == null || sharedBody.isEmpty()) {
            return "";
        }
        if (resolved) {
            return escape(resolve(sharedBody, module));
        }
        StringBuilder out = new StringBuilder(sharedBody.length() + 64);
        int start = 0;
        int index;
        while ((index = sharedBody.indexOf(MODULE_PLACEHOLDER, start)) >= 0) {
            out.append(escape(sharedBody.substring(start, index)));
            out.append("<span class=\"placeholder\">").append(escape(MODULE_PLACEHOLDER)).append("</span>");
            start = index + MODULE_PLACEHOLDER.length();
        }
        out.append(escape(sharedBody.substring(start)));
        return out.toString();
    }

    /** Documento HTML completo listo para {@code WebView.loadContent}. */
    public static String toDocument(String sharedBody, String module, boolean resolved) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>"
            + STYLES + "</style></head><body><pre>"
            + toHtml(sharedBody, module, resolved) + "</pre></body></html>";
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
