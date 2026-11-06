package es.vargontoc.agentic.viewer.terminal;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstracción sobre una sesión PTY. Permite intercambiar la implementación
 * real (pty4j) por un doble de prueba sin dependencias nativas.
 */
public interface PtySession extends AutoCloseable {

    /** Arranca el proceso en el PTY. */
    void start() throws Exception;

    /** Flujo de salida del proceso (lo que el proceso escribe). */
    InputStream getInputStream();

    /** Flujo de entrada del proceso (lo que el usuario envía). */
    OutputStream getOutputStream();

    /** Redimensiona la terminal virtual. */
    void resize(int columns, int rows);

    /** Envía una interrupción (Ctrl+C) al proceso. */
    void interrupt();

    /** Cierra la sesión y destruye el proceso. */
    @Override
    void close();

    /** Espera a que el proceso termine y devuelve su código de salida. */
    int waitFor() throws InterruptedException;

    /** Indica si el proceso sigue vivo. */
    boolean isAlive();
}
