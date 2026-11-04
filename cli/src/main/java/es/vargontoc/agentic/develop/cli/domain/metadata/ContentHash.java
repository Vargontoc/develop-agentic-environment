package es.vargontoc.agentic.develop.cli.domain.metadata;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Hash del contenido lógico de un artefacto. El fin de línea se normaliza para que CRLF y LF den el mismo hash
 * y un cambio de estilo de línea no se confunda con una edición externa.
 */
public final class ContentHash {

    private ContentHash() {}

    /**
     * Hash de un conjunto de ficheros (una skill) a partir del hash de cada uno: depende de las rutas relativas
     * y de esos hashes, no del orden en que se recorren.
     */
    public static String ofFiles(Map<String, String> hashesByRelativePath) {
        StringBuilder combined = new StringBuilder();
        new TreeMap<>(hashesByRelativePath).forEach((path, hash) ->
            combined.append(path.replace('\\', '/')).append('\0').append(hash).append('\n'));
        return of(combined.toString());
    }

    /** Hash de un fichero binario, sin normalizar nada. */
    public static String ofBytes(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String of(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
