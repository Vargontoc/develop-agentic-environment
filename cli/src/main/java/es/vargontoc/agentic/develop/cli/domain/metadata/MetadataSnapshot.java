package es.vargontoc.agentic.develop.cli.domain.metadata;

import java.util.Map;
import java.util.TreeMap;

/**
 * Contenido completo del almacén. Los mapas van ordenados para que el fichero sea determinista.
 */
public record MetadataSnapshot(int schemaVersion, Map<String, GroupRecord> groups, Map<String, ArtifactRecord> artifacts) {

    public MetadataSnapshot {
        groups = groups == null ? new TreeMap<>() : new TreeMap<>(groups);
        artifacts = artifacts == null ? new TreeMap<>() : new TreeMap<>(artifacts);
    }

    public static MetadataSnapshot empty(int schemaVersion) {
        return new MetadataSnapshot(schemaVersion, Map.of(), Map.of());
    }
}
