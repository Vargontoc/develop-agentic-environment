package es.vargontoc.agentic.develop.cli.domain.metadata;

import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;

/**
 * Grupo lógico: identidad compartida por varios artefactos de un mismo ámbito.
 *
 * @param sharedBody cuerpo compartido; en los especialistas de proyecto conserva el placeholder del módulo
 */
public record GroupRecord(String id, ArtifactScope scope, String sharedBody) {}
