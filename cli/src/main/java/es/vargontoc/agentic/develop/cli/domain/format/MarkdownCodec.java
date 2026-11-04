package es.vargontoc.agentic.develop.cli.domain.format;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;

final class MarkdownCodec {

    private static final String DELIMITER = "---";

    private MarkdownCodec() {}

    /**
     * Cabecera = frontmatter completo más las líneas en blanco que lo siguen; cuerpo = el resto, sin tocar.
     */
    static ArtifactParts split(String text) {
        int firstEnd = lineEnd(text, 0);
        if(!DELIMITER.equals(content(text, 0, firstEnd)))
            throw new DomainException(AppErrorCode.ARTIFACT_FORMAT_INVALID);

        int pos = firstEnd;
        while(pos < text.length()) {
            int end = lineEnd(text, pos);
            if(DELIMITER.equals(content(text, pos, end))) {
                return partsFrom(text, skipBlankLines(text, end));
            }
            pos = end;
        }
        throw new DomainException(AppErrorCode.ARTIFACT_FORMAT_INVALID);
    }

    static String compose(ArtifactParts parts) {
        String header = parts.header();
        if(parts.body().isEmpty())
            return header;
        return header.endsWith("\n") ? header + parts.body() : header + "\n\n" + parts.body();
    }

    private static ArtifactParts partsFrom(String text, int split) {
        return new ArtifactParts(text.substring(0, split), text.substring(split));
    }

    private static int skipBlankLines(String text, int from) {
        int pos = from;
        while(pos < text.length()) {
            int end = lineEnd(text, pos);
            if(!content(text, pos, end).isBlank())
                break;
            pos = end;
        }
        return pos;
    }

    // Índice justo después del terminador de línea (\n o \r\n), o el final del texto
    private static int lineEnd(String text, int from) {
        int nl = text.indexOf('\n', from);
        return nl < 0 ? text.length() : nl + 1;
    }

    private static String content(String text, int from, int end) {
        String line = text.substring(from, end);
        if(line.endsWith("\n"))
            line = line.substring(0, line.length() - 1);
        if(line.endsWith("\r"))
            line = line.substring(0, line.length() - 1);
        return line;
    }
}
