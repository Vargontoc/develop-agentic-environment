package es.vargontoc.agentic.viewer.cli.model;

/**
 * Resultado del comando {@code adopt}. Sin confirmar, {@code dryRun} es true y
 * {@code applied} false; con confirmación, {@code applied} indica si se escribió.
 */
public record AdoptData(String path, String groupId, boolean dryRun, boolean applied, String diff, String backup) {
}
