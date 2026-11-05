package es.vargontoc.agentic.viewer.cli;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta el CLI con {@link ProcessBuilder}, sin shell, y devuelve stdout,
 * stderr, código de salida y si se agotó el tiempo. La lectura de las dos
 * salidas se hace en hilos separados para evitar bloqueos.
 */
public final class CliProcess implements CliRunner {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final CliCommand command;

    public CliProcess(CliCommand command) {
        this.command = command;
    }

    @Override
    public CliResult run(List<String> args) {
        return run(args, null, DEFAULT_TIMEOUT);
    }

    @Override
    public CliResult run(List<String> args, String stdin, Duration timeout) {
        ProcessBuilder builder = new ProcessBuilder(command.command(args));
        builder.redirectErrorStream(false);
        builder.environment().remove("JAVA_TOOL_OPTIONS");
        builder.environment().remove("JDK_JAVA_OPTIONS");

        long start = System.nanoTime();
        Process process;
        try {
            process = builder.start();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo iniciar el CLI: " + exception.getMessage(), exception);
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Thread outReader = Thread.ofVirtual().start(() -> read(process.getInputStream(), stdout));
        Thread errReader = Thread.ofVirtual().start(() -> read(process.getErrorStream(), stderr));

        String stdinError = writeStdin(process, stdin);

        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IllegalStateException("La ejecución del CLI fue interrumpida", exception);
        }

        if (!finished) {
            process.destroyForcibly();
            try {
                process.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        join(outReader);
        join(errReader);

        if (stdinError != null) {
            stderr.append(stdinError);
        }

        int exitCode = process.isAlive() ? -1 : process.exitValue();
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        return new CliResult(exitCode, stdout.toString(), stderr.toString(), !finished, durationMillis);
    }

    private static String writeStdin(Process process, String stdin) {
        if (stdin == null) {
            return null;
        }
        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(stdin);
            return null;
        } catch (IOException exception) {
            return "[stdin] " + exception.getMessage();
        }
    }

    private static void read(InputStream input, StringBuilder target) {
        try (InputStream stream = input) {
            target.append(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            target.append("[lectura] ").append(exception.getMessage());
        }
    }

    private static void join(Thread thread) {
        try {
            thread.join(2000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public record CliResult(int exitCode, String stdout, String stderr, boolean timedOut, long durationMillis) {
    }
}
