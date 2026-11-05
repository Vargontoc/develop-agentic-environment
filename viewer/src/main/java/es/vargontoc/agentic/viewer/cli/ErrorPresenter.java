package es.vargontoc.agentic.viewer.cli;

/**
 * Traduce los códigos estables del contrato a una acción sugerida para el
 * usuario, conservando el código, la ruta y el mensaje originales.
 */
public final class ErrorPresenter {

    public record ErrorNotice(String code, String path, String message, String action) {
    }

    private ErrorPresenter() {}

    public static ErrorNotice present(CliClientException error) {
        return new ErrorNotice(
            error.code(),
            error.path(),
            error.getMessage(),
            actionFor(error.code()));
    }

    private static String actionFor(String code) {
        if (code == null || code.isBlank()) {
            return "Reintentar y revisar los logs del CLI.";
        }
        if (code.startsWith("client.")) {
            return switch (code) {
                case "client.timeout" -> "Reintentar la operación.";
                case "client.empty_output" -> "Comprobar que el jar del CLI es el correcto.";
                case "client.invalid_json", "client.invalid_data", "client.invalid_error" ->
                    "Revisar la versión del CLI y del viewer.";
                case "client.unsupported_schema" -> "Actualizar el viewer o el CLI para usar la misma versión.";
                default -> "Reintentar y revisar los logs del CLI.";
            };
        }
        if (code.startsWith("usage.")) {
            return "Revisar la versión del CLI o los argumentos de la operación.";
        }
        if (code.startsWith("internal.")) {
            return "Reintentar; si persiste, revisar los logs del CLI.";
        }
        if (code.startsWith("validation.project.") || code.equals("not_found.project.path")) {
            return "Seleccionar un directorio existente y válido.";
        }
        if (code.startsWith("validation.module.") || code.equals("not_found.module")) {
            return "Revisar el nombre o actualizar el árbol para elegir un módulo registrado.";
        }
        if (code.equals("conflict.artifact.modified_externally")) {
            return "Recargar el artefacto o confirmar la sobrescritura.";
        }
        if (code.equals("conflict.artifact.already_exists")) {
            return "Elegir otro nombre o confirmar la sobrescritura.";
        }
        if (code.equals("conflict.artifact.already_tracked")) {
            return "Recargar el estado del artefacto.";
        }
        if (code.equals("not_found.group")) {
            return "Adoptar el artefacto en su grupo antes de propagar.";
        }
        if (code.equals("validation.artifact.format_invalid")) {
            return "Corregir la cabecera o el cuerpo según el formato.";
        }
        if (code.equals("validation.artifact.not_editable")) {
            return "Elegir un artefacto instalado, no un recurso del jar.";
        }
        if (code.equals("validation.artifact.not_catalogued")) {
            return "Elegir un artefacto del catálogo.";
        }
        if (code.equals("validation.artifact.scope_unsupported")) {
            return "Elegir un proveedor con soporte para el ámbito seleccionado.";
        }
        if (code.equals("validation.artifact.kind_mismatch")) {
            return "Elegir el tipo correcto: agente o skill.";
        }
        if (code.equals("validation.content.unreadable")) {
            return "Comprobar que el fichero es UTF-8 y se puede leer.";
        }
        if (code.equals("validation.path.out_of_scope")) {
            return "Elegir una ruta dentro del proyecto o del proveedor.";
        }
        return "Reintentar y revisar los logs del CLI.";
    }
}
