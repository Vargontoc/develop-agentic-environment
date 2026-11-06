package es.vargontoc.agentic.viewer.terminal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sesión PTY falsa para pruebas. Simula un proceso que responde a la entrada.
 */
public final class FakePtySession implements PtySession {

    private final ByteArrayOutputStream inputCapture = new ByteArrayOutputStream();
    private ByteArrayInputStream outputSource;
    private final AtomicBoolean alive = new AtomicBoolean(false);
    private int lastColumns;
    private int lastRows;
    private boolean interrupted;
    private boolean closed;
    private String responseText;

    /** Crea una sesión con el texto de respuesta predefinido. */
    public FakePtySession(String responseText) {
        this.responseText = responseText == null ? "" : responseText;
    }

    @Override
    public void start() {
        outputSource = new ByteArrayInputStream(responseText.getBytes());
        alive.set(true);
    }

    @Override
    public InputStream getInputStream() {
        return outputSource;
    }

    @Override
    public OutputStream getOutputStream() {
        return inputCapture;
    }

    @Override
    public void resize(int columns, int rows) {
        this.lastColumns = columns;
        this.lastRows = rows;
    }

    @Override
    public void interrupt() {
        this.interrupted = true;
    }

    @Override
    public void close() {
        this.closed = true;
        alive.set(false);
    }

    @Override
    public int waitFor() throws InterruptedException {
        while (alive.get()) {
            Thread.sleep(50);
        }
        return 0;
    }

    @Override
    public boolean isAlive() {
        return alive.get();
    }

    /** Termina la sesión simulada. */
    public void finish() {
        alive.set(false);
    }

    public String getSentInput() {
        return inputCapture.toString();
    }

    public int getLastColumns() {
        return lastColumns;
    }

    public int getLastRows() {
        return lastRows;
    }

    public boolean wasInterrupted() {
        return interrupted;
    }

    public boolean wasClosed() {
        return closed;
    }
}
