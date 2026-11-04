package es.vargontoc.agentic.develop.cli.domain.metadata;

/**
 * Lo que el almacén recuerda de un artefacto generado.
 *
 * @param path    ruta absoluta normalizada del fichero o directorio
 * @param groupId grupo lógico al que pertenece
 * @param hash    hash de la última composición conocida (ver {@link ContentHash})
 */
public record ArtifactRecord(String path, String groupId, String hash, LinkState state) {

    public static ArtifactRecord of(String path, String groupId, String content, LinkState state) {
        return new ArtifactRecord(path, groupId, ContentHash.of(content), state);
    }

    public static ArtifactRecord ofHash(String path, String groupId, String hash, LinkState state) {
        return new ArtifactRecord(path, groupId, hash, state);
    }

    public ArtifactRecord withHash(String content) {
        return new ArtifactRecord(path, groupId, ContentHash.of(content), state);
    }

    public ArtifactRecord withState(LinkState newState) {
        return new ArtifactRecord(path, groupId, hash, newState);
    }
}
