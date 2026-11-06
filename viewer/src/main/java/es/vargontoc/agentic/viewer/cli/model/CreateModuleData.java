package es.vargontoc.agentic.viewer.cli.model;

import java.util.List;

/**
 * Respuesta de los comandos {@code create-module} y {@code add-technician}.
 *
 * @param module    Nombre del módulo
 * @param dryRun    true si fue simulación
 * @param created   Archivos/rutas creadas
 * @param skipped   Archivos/rutas omitidas (ya existían)
 * @param warnings  Avisos de la operación
 */
public record CreateModuleData(
    String module,
    boolean dryRun,
    List<String> created,
    List<String> skipped,
    List<String> warnings) {
}
