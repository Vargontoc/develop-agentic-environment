package es.vargontoc.agentic.develop.cli.infrastructure.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

/**
 * Lee y escribe texto UTF-8 dentro de los ámbitos permitidos.
 * Las escrituras conservan el fin de línea del fichero existente, son atómicas (temporal + movimiento)
 * y dejan una copia de seguridad recuperable del contenido anterior.
 */
public final class TextStore {

    /** Contenido leído, con el fin de línea detectado ("\n" o "\r\n"). */
    public record TextFile(String content, String lineEnding) {}

    /** {@code written} es falso si el contenido no cambia; {@code backup} es null si no había nada que respaldar. */
    public record WriteResult(boolean written, Path backup) {}

    @FunctionalInterface
    interface Mover {
        void move(Path source, Path target) throws IOException;
    }

    private static final String LF = "\n";
    private static final String CRLF = "\r\n";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS").withZone(ZoneOffset.UTC);

    private final PathResolver paths;
    private final List<Path> scopes;
    private final Path backupRoot;
    private final Mover mover;

    public TextStore(PathResolver paths, List<Path> scopes, Path backupRoot) {
        this(paths, scopes, backupRoot, TextStore::atomicMove);
    }

    TextStore(PathResolver paths, List<Path> scopes, Path backupRoot, Mover mover) {
        this.paths = paths;
        this.scopes = List.copyOf(scopes);
        this.backupRoot = backupRoot;
        this.mover = mover;
    }

    public TextFile read(Path path) {
        requireInScope(path);
        try {
            String content = decode(Files.readAllBytes(path));
            return new TextFile(content, detectLineEnding(content));
        } catch(IOException e) {
            throw new DomainException(AppErrorCode.TEXT_READ_FAILED, e);
        }
    }

    public WriteResult write(Path path, String content) {
        requireInScope(path);
        try {
            boolean exists = Files.exists(path);
            String existing = exists ? decode(Files.readAllBytes(path)) : null;
            String lineEnding = exists ? detectLineEnding(existing) : LF;

            String text = withLineEnding(content, lineEnding);
            if(text.equals(existing))
                return new WriteResult(false, null);

            Files.createDirectories(path.toAbsolutePath().getParent());
            Path temp = Files.createTempFile(path.toAbsolutePath().getParent(), ".tmp-", ".part");
            try {
                Files.write(temp, text.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
                Path backup = exists ? backup(path) : null;
                mover.move(temp, path);
                return new WriteResult(true, backup);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch(IOException e) {
            throw new DomainException(AppErrorCode.TEXT_WRITE_FAILED, e);
        }
    }

    private void requireInScope(Path path) {
        DomainException last = null;
        for(Path scope : scopes) {
            try {
                paths.requireWithin(scope, path);
                return;
            } catch(DomainException e) {
                last = e;
            }
        }
        throw last != null ? last : new DomainException(AppErrorCode.PATH_OUT_OF_SCOPE);
    }

    // Copia con marca de tiempo y un resumen de la ruta original para poder reconocerla
    private Path backup(Path original) throws IOException {
        Files.createDirectories(backupRoot);
        String base = STAMP.format(Instant.now()) + "-" + pathDigest(original) + "-" + original.getFileName();
        Path target = backupRoot.resolve(base + ".bak");
        for(int n = 1; Files.exists(target); n++)
            target = backupRoot.resolve(base + "." + n + ".bak");
        return Files.copy(original, target);
    }

    private static String pathDigest(Path path) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(path.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 4);
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch(AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String decode(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString();
    }

    private static String detectLineEnding(String content) {
        return content.contains(CRLF) ? CRLF : LF;
    }

    private static String withLineEnding(String content, String lineEnding) {
        String normalized = content.replace(CRLF, LF).replace("\r", LF);
        return CRLF.equals(lineEnding) ? normalized.replace(LF, CRLF) : normalized;
    }
}
