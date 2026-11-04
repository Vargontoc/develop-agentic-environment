package es.vargontoc.agentic.develop.cli.infrastructure.metadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.metadata.ArtifactRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.domain.metadata.MetadataSnapshot;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Agrupación lógica, cuerpos compartidos, hashes y vinculación de los artefactos generados.
 * Vive en un único fichero JSON del home de la aplicación; nunca escribe nada dentro de un proyecto ni toca artefactos.
 * Un almacén ausente, corrupto o de una versión no soportada se trata como vacío: los artefactos pasan a ser legacy.
 */
public final class MetadataStore {

    public static final int SCHEMA_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(MetadataStore.class);

    private final Path file;
    private final TextStore text;
    private final JsonMapper mapper = JsonMapperFactory.create();

    private MetadataSnapshot snapshot;

    /** {@code text} debe permitir escribir en el directorio de {@code file}. */
    public MetadataStore(Path file, TextStore text) {
        this.file = file;
        this.text = text;
    }

    public MetadataSnapshot snapshot() {
        if(snapshot == null)
            snapshot = read();
        return snapshot;
    }

    public Optional<ArtifactRecord> artifact(Path path) {
        return Optional.ofNullable(snapshot().artifacts().get(key(path)));
    }

    public Optional<GroupRecord> group(String id) {
        return Optional.ofNullable(snapshot().groups().get(id));
    }

    public List<ArtifactRecord> artifactsOfGroup(String groupId) {
        return snapshot().artifacts().values().stream()
            .filter(a -> a.groupId().equals(groupId))
            .toList();
    }

    public void putGroup(GroupRecord group) {
        MetadataSnapshot current = snapshot();
        current.groups().put(group.id(), group);
        persist(current);
    }

    /** Registra (o actualiza) un artefacto con el hash de su contenido. */
    public void putArtifact(Path path, String groupId, String content, LinkState state) {
        MetadataSnapshot current = snapshot();
        current.artifacts().put(key(path), ArtifactRecord.of(key(path), groupId, content, state));
        persist(current);
    }

    /** Como {@link #putArtifact} pero con el hash ya calculado (skills, que son varios ficheros). */
    public void putArtifactHash(Path path, String groupId, String hash, LinkState state) {
        MetadataSnapshot current = snapshot();
        current.artifacts().put(key(path), ArtifactRecord.ofHash(key(path), groupId, hash, state));
        persist(current);
    }

    public void markDetached(Path path) {
        MetadataSnapshot current = snapshot();
        ArtifactRecord record = current.artifacts().get(key(path));
        if(record == null || record.state() == LinkState.DETACHED)
            return;
        current.artifacts().put(key(path), record.withState(LinkState.DETACHED));
        persist(current);
    }

    public void removeArtifact(Path path) {
        MetadataSnapshot current = snapshot();
        if(current.artifacts().remove(key(path)) != null)
            persist(current);
    }

    /** Ruta normalizada con la que se identifica un artefacto en el almacén. */
    public static String key(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private MetadataSnapshot read() {
        if(!Files.exists(file))
            return MetadataSnapshot.empty(SCHEMA_VERSION);

        try {
            String content = text.read(file).content();

            if(!(mapper.readValue(content, Map.class).get("schemaVersion") instanceof Number version)
                    || version.intValue() != SCHEMA_VERSION) {
                log.warn("Metadata store '{}' has an unsupported schema version; treating artifacts as custom", file);
                return MetadataSnapshot.empty(SCHEMA_VERSION);
            }

            MetadataSnapshot loaded = mapper.readValue(content, MetadataSnapshot.class);
            if(!isConsistent(loaded)) {
                log.warn("Metadata store '{}' is inconsistent; treating artifacts as custom", file);
                return MetadataSnapshot.empty(SCHEMA_VERSION);
            }
            return loaded;
        } catch(RuntimeException e) {
            log.warn("Metadata store '{}' is unreadable; treating artifacts as custom: {}", file, e.getMessage());
            return MetadataSnapshot.empty(SCHEMA_VERSION);
        }
    }

    private static boolean isConsistent(MetadataSnapshot loaded) {
        boolean groupsOk = loaded.groups().entrySet().stream()
            .allMatch(e -> e.getValue() != null && e.getKey().equals(e.getValue().id()) && e.getValue().scope() != null);
        boolean artifactsOk = loaded.artifacts().entrySet().stream()
            .allMatch(e -> e.getValue() != null && e.getKey().equals(e.getValue().path())
                && e.getValue().groupId() != null && e.getValue().hash() != null && e.getValue().state() != null);
        return groupsOk && artifactsOk;
    }

    private void persist(MetadataSnapshot current) {
        MetadataSnapshot toWrite = new MetadataSnapshot(SCHEMA_VERSION, current.groups(), current.artifacts());
        text.write(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toWrite) + "\n");
        snapshot = toWrite;
    }
}
