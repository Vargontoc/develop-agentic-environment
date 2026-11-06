package es.vargontoc.agentic.viewer.terminal;

import java.util.regex.Pattern;

/**
 * Elimina secuencias de escape ANSI de la salida del PTY para mostrarla
 * como texto plano en la consola JavaFX.
 */
public final class AnsiStripper {

    // Secuencias CSI (Control Sequence Introducer): ESC [ ... final_byte
    private static final Pattern CSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*[A-Za-z]");

    // Secuencias OSC (Operating System Command): ESC ] ... ST
    private static final Pattern OSC_PATTERN = Pattern.compile("\u001B\\].*?(?:\u0007|\u001B\\\\)");

    // Otros caracteres de control simples (ESC + letra)
    private static final Pattern ESC_PATTERN = Pattern.compile("\u001B[A-Za-z]");

    // Carácter ESC individual
    private static final Pattern BARE_ESC_PATTERN = Pattern.compile("\u001B");

    private AnsiStripper() {}

    /**
     * Elimina las secuencias ANSI del texto dado.
     */
    public static String strip(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        result = OSC_PATTERN.matcher(result).replaceAll("");
        result = CSI_PATTERN.matcher(result).replaceAll("");
        result = ESC_PATTERN.matcher(result).replaceAll("");
        result = BARE_ESC_PATTERN.matcher(result).replaceAll("");
        return result;
    }
}
