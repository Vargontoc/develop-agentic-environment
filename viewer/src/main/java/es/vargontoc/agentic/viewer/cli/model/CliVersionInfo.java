package es.vargontoc.agentic.viewer.cli.model;

/**
 * Resultado de la sonda de versión del CLI.
 *
 * @param version       versión del CLI tal como la devuelve {@code --version}
 * @param schemaVersion versión del contrato leída del envelope; {@code -1} si no vino
 */
public record CliVersionInfo(String version, int schemaVersion) {}
