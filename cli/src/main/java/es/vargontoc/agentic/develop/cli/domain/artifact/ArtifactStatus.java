package es.vargontoc.agentic.develop.cli.domain.artifact;

/**
 * Estado de vinculación de un artefacto instalado.
 * {@code LEGACY} es el que no tiene metadatos: se trata como personalizado y no se propaga sobre él.
 */
public enum ArtifactStatus {
    LINKED, DETACHED, LEGACY
}
