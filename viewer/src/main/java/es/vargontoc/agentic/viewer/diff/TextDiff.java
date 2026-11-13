package es.vargontoc.agentic.viewer.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Diff de líneas local para resolver conflictos sin depender del CLI.
 * Genera líneas con prefijo {@code -} (versión actual), {@code +} (versión nueva)
 * y espacio (sin cambios), el mismo formato que colorea {@link DiffHighlighter}.
 */
public final class TextDiff {

    // Límite de celdas de la matriz LCS para no agotar memoria con ficheros enormes.
    private static final long MAX_CELLS = 4_000_000L;

    private TextDiff() {}

    /** Diff entre {@code oldText} (por ejemplo, el disco) y {@code newText} (tu versión). */
    public static String between(String oldText, String newText) {
        List<String> oldLines = lines(oldText);
        List<String> newLines = lines(newText);
        if (oldLines.isEmpty() && newLines.isEmpty()) {
            return "";
        }
        if ((long) (oldLines.size() + 1) * (newLines.size() + 1) > MAX_CELLS) {
            return fullReplacement(oldLines, newLines);
        }

        int[][] lcs = new int[oldLines.size() + 1][newLines.size() + 1];
        for (int i = oldLines.size() - 1; i >= 0; i--) {
            for (int j = newLines.size() - 1; j >= 0; j--) {
                lcs[i][j] = oldLines.get(i).equals(newLines.get(j))
                    ? lcs[i + 1][j + 1] + 1
                    : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }

        StringBuilder out = new StringBuilder();
        int i = 0;
        int j = 0;
        while (i < oldLines.size() && j < newLines.size()) {
            if (oldLines.get(i).equals(newLines.get(j))) {
                out.append(' ').append(oldLines.get(i)).append('\n');
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                out.append('-').append(oldLines.get(i)).append('\n');
                i++;
            } else {
                out.append('+').append(newLines.get(j)).append('\n');
                j++;
            }
        }
        while (i < oldLines.size()) {
            out.append('-').append(oldLines.get(i++)).append('\n');
        }
        while (j < newLines.size()) {
            out.append('+').append(newLines.get(j++)).append('\n');
        }
        return out.toString();
    }

    private static List<String> lines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        String[] parts = text.split("\\R", -1);
        List<String> lines = new ArrayList<>(List.of(parts));
        // Un salto final no es una línea vacía adicional en el diff.
        if (!lines.isEmpty() && lines.getLast().isEmpty()) {
            lines.removeLast();
        }
        return lines;
    }

    private static String fullReplacement(List<String> oldLines, List<String> newLines) {
        StringBuilder out = new StringBuilder();
        for (String line : oldLines) {
            out.append('-').append(line).append('\n');
        }
        for (String line : newLines) {
            out.append('+').append(line).append('\n');
        }
        return out.toString();
    }
}
