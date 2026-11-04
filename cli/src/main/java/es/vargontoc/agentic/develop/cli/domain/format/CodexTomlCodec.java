package es.vargontoc.agentic.develop.cli.domain.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;

/**
 * Agente de Codex: {@code developer_instructions = """cuerpo"""}.
 * Cabecera = todo hasta las comillas de apertura (incluidas); cola = lo que sigue a las de cierre.
 * El cuerpo se expone sin escapes y se escapa al componer ({@code \} y {@code """}).
 */
final class CodexTomlCodec {

    static final String DELIMITER = "\"\"\"";

    private static final Pattern OPENING = Pattern.compile("(?m)^developer_instructions[ \\t]*=[ \\t]*\"\"\"");
    private static final int MAX_QUOTE_RUN = 5;

    private CodexTomlCodec() {}

    static ArtifactParts split(String text) {
        Matcher opening = OPENING.matcher(text);
        if(!opening.find())
            throw invalid();

        int bodyStart = opening.end();
        int bodyEnd = closingIndex(text, bodyStart);

        return new ArtifactParts(
            text.substring(0, bodyStart),
            unescape(text.substring(bodyStart, bodyEnd)),
            text.substring(bodyEnd + DELIMITER.length()));
    }

    static String compose(ArtifactParts parts) {
        return parts.header() + escape(parts.body()) + DELIMITER + parts.suffix();
    }

    // Una racha de 3 a 5 comillas cierra la cadena; las sobrantes del principio pertenecen al cuerpo
    private static int closingIndex(String text, int from) {
        int i = from;
        while(i < text.length()) {
            char c = text.charAt(i);
            if(c == '\\') {
                i += 2;
            } else if(c == '"') {
                int run = quoteRun(text, i);
                if(run >= 3)
                    return i + Math.min(run, MAX_QUOTE_RUN) - 3;
                i += run;
            } else {
                i++;
            }
        }
        throw invalid();
    }

    private static int quoteRun(String text, int from) {
        int end = from;
        while(end < text.length() && text.charAt(end) == '"')
            end++;
        return end - from;
    }

    static String escape(String body) {
        String escaped = body.replace("\\", "\\\\").replace(DELIMITER, "\\\"\\\"\\\"");

        // Una comilla final sin escapar se fundiría con las de cierre
        if(escaped.endsWith("\"") && trailingBackslashesBeforeLast(escaped) % 2 == 0)
            escaped = escaped.substring(0, escaped.length() - 1) + "\\\"";
        return escaped;
    }

    private static int trailingBackslashesBeforeLast(String text) {
        int count = 0;
        for(int i = text.length() - 2; i >= 0 && text.charAt(i) == '\\'; i--)
            count++;
        return count;
    }

    static String unescape(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        int i = 0;
        while(i < raw.length()) {
            char c = raw.charAt(i);
            if(c != '\\') {
                out.append(c);
                i++;
                continue;
            }
            i = appendEscape(raw, i + 1, out);
        }
        return out.toString();
    }

    // Devuelve el índice siguiente a la secuencia de escape que empieza en 'at' (justo tras la barra)
    private static int appendEscape(String raw, int at, StringBuilder out) {
        if(at >= raw.length())
            throw invalid();

        char c = raw.charAt(at);
        switch(c) {
            case 'b': out.append('\b'); return at + 1;
            case 't': out.append('\t'); return at + 1;
            case 'n': out.append('\n'); return at + 1;
            case 'f': out.append('\f'); return at + 1;
            case 'r': out.append('\r'); return at + 1;
            case '"': out.append('"'); return at + 1;
            case '\\': out.append('\\'); return at + 1;
            case 'u': return appendUnicode(raw, at + 1, 4, out);
            case 'U': return appendUnicode(raw, at + 1, 8, out);
            default: return skipLineEnding(raw, at);
        }
    }

    // Barra al final de línea: elimina el salto y todo el blanco que le sigue
    private static int skipLineEnding(String raw, int at) {
        int i = at;
        while(i < raw.length() && (raw.charAt(i) == ' ' || raw.charAt(i) == '\t'))
            i++;
        if(i >= raw.length() || (raw.charAt(i) != '\n' && raw.charAt(i) != '\r'))
            throw invalid();
        while(i < raw.length() && Character.isWhitespace(raw.charAt(i)))
            i++;
        return i;
    }

    private static int appendUnicode(String raw, int from, int digits, StringBuilder out) {
        int end = from + digits;
        if(end > raw.length())
            throw invalid();
        try {
            out.appendCodePoint(Integer.parseInt(raw.substring(from, end), 16));
        } catch(IllegalArgumentException e) {
            throw invalid();
        }
        return end;
    }

    private static DomainException invalid() {
        return new DomainException(AppErrorCode.ARTIFACT_FORMAT_INVALID);
    }
}
