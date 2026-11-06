package es.vargontoc.agentic.viewer.terminal;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

/**
 * Sesión PTY real basada en pty4j. Ejecuta el CLI interactivo con el
 * directorio del proyecto como directorio de trabajo.
 */
public final class Pty4jSession implements PtySession {

    private final List<String> command;
    private final Path workingDirectory;
    private final int initialColumns;
    private final int initialRows;
    private PtyProcess process;

    public Pty4jSession(List<String> command, Path workingDirectory, int columns, int rows) {
        this.command = List.copyOf(command);
        this.workingDirectory = workingDirectory;
        this.initialColumns = Math.max(columns, 20);
        this.initialRows = Math.max(rows, 5);
    }

    @Override
    public void start() throws Exception {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm");
        env.remove("JAVA_TOOL_OPTIONS");
        env.remove("JDK_JAVA_OPTIONS");

        PtyProcessBuilder builder = new PtyProcessBuilder()
            .setCommand(command.toArray(new String[0]))
            .setEnvironment(env)
            .setDirectory(workingDirectory.toString())
            .setConsole(false);

        process = builder.start();
        process.setWinSize(new WinSize(initialColumns, initialRows));
    }

    @Override
    public InputStream getInputStream() {
        return process.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() {
        return process.getOutputStream();
    }

    @Override
    public void resize(int columns, int rows) {
        if (process != null && process.isAlive()) {
            process.setWinSize(new WinSize(Math.max(columns, 20), Math.max(rows, 5)));
        }
    }

    @Override
    public void interrupt() {
        if (process != null && process.isAlive()) {
            // Ctrl+C es el carácter 0x03 (ETX)
            try {
                process.getOutputStream().write(3);
                process.getOutputStream().flush();
            } catch (Exception exception) {
                // Si no se puede enviar, destruir el proceso
                process.destroyForcibly();
            }
        }
    }

    @Override
    public void close() {
        if (process != null) {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            try {
                process.getInputStream().close();
            } catch (Exception ignored) {
            }
            try {
                process.getOutputStream().close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public int waitFor() throws InterruptedException {
        if (process == null) {
            return -1;
        }
        return process.waitFor();
    }

    @Override
    public boolean isAlive() {
        return process != null && process.isAlive();
    }
}
