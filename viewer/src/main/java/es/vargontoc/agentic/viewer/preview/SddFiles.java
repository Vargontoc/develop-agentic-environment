package es.vargontoc.agentic.viewer.preview;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Lectura directa y en solo lectura de los ficheros de texto de {@code .sdd}.
 * Es la excepción documentada del contrato: {@code team.md}, {@code producto.md}
 * y {@code architecture.md} son texto del usuario sin metadatos propios del CLI.
 * Nunca escribe nada.
 */
public final class SddFiles {

    public static final String SDD_DIR = ".sdd";

    private SddFiles() {}

    /** Lista los ficheros de texto (Markdown/TOML) del directorio, ordenados; vacío si no existe. */
    public static List<Path> list(Path sddDir) {
        if (sddDir == null || !Files.isDirectory(sddDir)) {
            return List.of();
        }
        try (var stream = Files.list(sddDir)) {
            return stream.filter(Files::isRegularFile)
                .filter(SddFiles::isTextFile)
                .sorted()
                .toList();
        } catch (IOException exception) {
            throw new SddReadException(sddDir, exception);
        }
    }

    /** Lee un fichero dentro del directorio {@code .sdd} como UTF-8, comprobando que no se sale de él. */
    public static String read(Path sddDir, Path file) {
        requireWithin(sddDir, file);
        if (!Files.isRegularFile(file)) {
            throw new SddReadException(file, "El fichero no existe o no es un fichero.");
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (CharacterCodingException exception) {
            throw new SddReadException(file, "El fichero no es UTF-8 válido.");
        } catch (IOException exception) {
            throw new SddReadException(file, exception);
        }
    }

    private static boolean isTextFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".md") || name.endsWith(".toml");
    }

    private static void requireWithin(Path sddDir, Path file) {
        Path dir = sddDir.toAbsolutePath().normalize();
        Path target = file.toAbsolutePath().normalize();
        if (!target.startsWith(dir)) {
            throw new SddReadException(file, "La ruta está fuera de .sdd.");
        }
    }

    /** Error controlado al leer un fichero de {@code .sdd}; lleva la ruta afectada. */
    public static final class SddReadException extends RuntimeException {

        private final Path path;

        public SddReadException(Path path, String message) {
            super(message);
            this.path = path;
        }

        public SddReadException(Path path, IOException cause) {
            super(cause.getMessage() == null ? "No se pudo leer el fichero." : cause.getMessage(), cause);
            this.path = path;
        }

        public Path path() {
            return path;
        }
    }
}
