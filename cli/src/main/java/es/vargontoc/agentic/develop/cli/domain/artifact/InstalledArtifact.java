package es.vargontoc.agentic.develop.cli.domain.artifact;

import java.nio.file.Path;

/**
 * Un artefacto de la aplicación que existe en disco.
 *
 * @param hash             hash del contenido actual (de todos sus ficheros si es una skill)
 * @param modifiedExternally true si hay un hash registrado y el actual es distinto
 */
public record InstalledArtifact(CatalogEntry entry, Path path, String hash, ArtifactStatus status, boolean modifiedExternally) {}
