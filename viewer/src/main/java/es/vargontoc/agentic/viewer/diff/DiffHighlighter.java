package es.vargontoc.agentic.viewer.diff;

/**
 * Clasifica las líneas de un diff para darles color en la interfaz. Es lógica
 * pura para poder probarla sin JavaFX.
 */
public final class DiffHighlighter {

    public static final String LINE = "diff-line";
    public static final String ADDED = "diff-add";
    public static final String REMOVED = "diff-del";
    public static final String HUNK = "diff-hunk";
    public static final String HEADER = "diff-header";
    public static final String TITLE = "diff-title";

    private DiffHighlighter() {}

    public static String styleClass(String line) {
        if (line == null || line.isBlank()) {
            return LINE;
        }
        if (line.startsWith("+++") || line.startsWith("---") || line.startsWith("diff ")
            || line.startsWith("Index: ") || line.startsWith("index ")) {
            return HEADER;
        }
        if (line.startsWith("@@")) {
            return HUNK;
        }
        if (line.startsWith("+")) {
            return ADDED;
        }
        if (line.startsWith("-")) {
            return REMOVED;
        }
        if (line.startsWith("Miembros:") || line.startsWith("Grupo:")
            || line.startsWith("actualizará") || line.startsWith("sin cambios")
            || line.startsWith("conflicto") || line.startsWith("personalizado")
            || line.startsWith("desvinculado") || line.startsWith("ausente")
            || line.startsWith("fuera de ámbito") || line.startsWith("falló")) {
            return TITLE;
        }
        return LINE;
    }
}
