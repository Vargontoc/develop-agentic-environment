package es.vargontoc.agentic.develop.cli.domain.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Diferencia por líneas entre dos textos. Solo devuelve las líneas que cambian, con prefijo {@code -} (quitada) o
 * {@code +} (añadida), en el orden en que aparecen. Los finales de línea no cuentan como diferencia.
 */
public final class TextDiff {

    // Por encima de este tamaño del tramo central no se busca la subsecuencia común: se da todo el tramo por cambiado
    private static final long MAX_CELLS = 4_000_000L;

    private TextDiff() {}

    public static String between(String before, String after) {
        List<String> a = lines(before);
        List<String> b = lines(after);

        int start = 0;
        while(start < a.size() && start < b.size() && a.get(start).equals(b.get(start)))
            start++;
        int endA = a.size();
        int endB = b.size();
        while(endA > start && endB > start && a.get(endA - 1).equals(b.get(endB - 1))) {
            endA--;
            endB--;
        }

        List<String> out = new ArrayList<>();
        diffMiddle(a.subList(start, endA), b.subList(start, endB), out);
        return String.join("\n", out);
    }

    private static void diffMiddle(List<String> a, List<String> b, List<String> out) {
        int n = a.size();
        int m = b.size();
        if((long) n * m > MAX_CELLS) {
            a.forEach(l -> out.add("-" + l));
            b.forEach(l -> out.add("+" + l));
            return;
        }

        int[][] lcs = new int[n + 1][m + 1];
        for(int i = n - 1; i >= 0; i--)
            for(int j = m - 1; j >= 0; j--)
                lcs[i][j] = a.get(i).equals(b.get(j)) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);

        int i = 0;
        int j = 0;
        while(i < n && j < m) {
            if(a.get(i).equals(b.get(j))) {
                i++;
                j++;
            } else if(lcs[i + 1][j] >= lcs[i][j + 1]) {
                out.add("-" + a.get(i++));
            } else {
                out.add("+" + b.get(j++));
            }
        }
        while(i < n)
            out.add("-" + a.get(i++));
        while(j < m)
            out.add("+" + b.get(j++));
    }

    private static List<String> lines(String text) {
        if(text.isEmpty())
            return List.of();
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        if(normalized.endsWith("\n"))
            normalized = normalized.substring(0, normalized.length() - 1);
        return List.of(normalized.split("\n", -1));
    }
}
