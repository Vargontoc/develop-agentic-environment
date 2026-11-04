package es.vargontoc.agentic.develop.cli.support;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

/** Ayuda de pruebas: ejecuta una acción y devuelve lo que escribió en la salida estándar. */
public final class Console {

    private Console() {}

    public static String capturedOut(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    public static <T> Captured<T> capture(Callable<T> action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            return new Captured<>(action.call(), buffer.toString(StandardCharsets.UTF_8));
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new IllegalStateException(e);
        } finally {
            System.setOut(original);
        }
    }

    public record Captured<T>(T value, String out) {}
}
