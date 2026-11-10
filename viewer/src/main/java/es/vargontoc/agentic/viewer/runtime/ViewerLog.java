package es.vargontoc.agentic.viewer.runtime;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Log de diagnóstico del visor. Escribe en la carpeta de estado para poder
 * revisar errores sin depender de la consola.
 */
public final class ViewerLog {

    private static final long MAX_BYTES = 1_000_000;
    private static final Object LOCK = new Object();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private ViewerLog() {}

    public static Path file() {
        return AppPaths.logFile();
    }

    public static void info(String message) {
        write("INFO", message, null);
    }

    public static void warn(String message) {
        write("WARN", message, null);
    }

    public static void error(String message, Throwable error) {
        write("ERROR", message, error);
    }

    private static void write(String level, String message, Throwable error) {
        String line = FORMAT.format(OffsetDateTime.now()) + " [" + level + "] " + message + System.lineSeparator();
        if (error != null) {
            StringWriter stack = new StringWriter();
            error.printStackTrace(new PrintWriter(stack));
            line += stack;
        }
        synchronized (LOCK) {
            try {
                Path file = file();
                Files.createDirectories(file.getParent());
                rotateIfNeeded(file);
                Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException | RuntimeException ignored) {
                // Un fallo al escribir el log no debe romper la aplicación.
            }
        }
    }

    private static void rotateIfNeeded(Path file) throws IOException {
        if (Files.exists(file) && Files.size(file) > MAX_BYTES) {
            Path previous = file.resolveSibling(file.getFileName() + ".1");
            Files.deleteIfExists(previous);
            Files.move(file, previous);
        }
    }
}
