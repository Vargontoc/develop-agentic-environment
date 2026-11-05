package es.vargontoc.agentic.develop.cli.domain.format;

import java.util.Locale;

/** Sistema operativo para el que se genera una cabecera. */
public enum TargetOs {
    WINDOWS, LINUX;

    /** El sistema en el que se ejecuta el CLI. */
    public static TargetOs current() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? WINDOWS : LINUX;
    }
}
