package es.vargontoc.agentic.develop.cli.command;

import java.util.List;

/**
 * Documento que se escribe en la salida estándar en modo no interactivo (REQ-004). Los campos y su orden no cambian
 * sin subir {@link #SCHEMA_VERSION}.
 *
 * @param command comando ejecutado, o el texto que el usuario escribió si no existe
 * @param data    resultado del comando; null si hubo error
 */
public record Envelope(int schemaVersion, String command, boolean ok, Object data, List<ErrorEntry> errors) {

    public static final int SCHEMA_VERSION = 1;

    /**
     * @param code    código estable, por ejemplo {@code validation.module.name_too_short} o {@code usage.unknown_command}
     * @param path    ruta o entrada afectada; null si no aplica
     * @param message mensaje comprensible, sin trazas
     */
    public record ErrorEntry(String code, String path, String message) {}

    public Envelope {
        errors = List.copyOf(errors);
    }

    public static Envelope success(String command, Object data) {
        return new Envelope(SCHEMA_VERSION, command, true, data, List.of());
    }

    public static Envelope failure(String command, ErrorEntry error) {
        return new Envelope(SCHEMA_VERSION, command, false, null, List.of(error));
    }
}
