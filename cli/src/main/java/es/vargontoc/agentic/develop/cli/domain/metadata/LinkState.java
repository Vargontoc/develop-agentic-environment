package es.vargontoc.agentic.develop.cli.domain.metadata;

/**
 * Estado de vinculación de un artefacto con su grupo. Un artefacto sin registro en el almacén no tiene estado:
 * es legacy y se trata como personalizado.
 */
public enum LinkState {
    LINKED, DETACHED
}
