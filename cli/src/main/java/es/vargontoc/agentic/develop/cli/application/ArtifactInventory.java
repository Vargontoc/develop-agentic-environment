package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactCatalog;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactStatus;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.domain.artifact.InstalledArtifact;
import es.vargontoc.agentic.develop.cli.domain.artifact.Inventory;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.metadata.ArtifactRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.ContentHash;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

/**
 * Descubre qué artefactos de la aplicación hay instalados. Parte del catálogo y comprueba cada ruta esperada:
 * nunca recorre la carpeta del proveedor, así que los agentes y skills ajenos no aparecen, y los recursos del jar
 * no instalados tampoco.
 */
public class ArtifactInventory {

    private final PathResolver paths;
    private final ArtifactCatalog catalog;
    private final MetadataStore metadata;
    private final Path backupRoot;

    public ArtifactInventory(PathResolver paths, ArtifactCatalog catalog, MetadataStore metadata, Path backupRoot) {
        this.paths = paths;
        this.catalog = catalog;
        this.metadata = metadata;
        this.backupRoot = backupRoot;
    }

    public Inventory scanGlobal(ProviderType provider) {
        return scan(paths.globalBase(provider), ArtifactScope.GLOBAL, provider, List.of());
    }

    /** {@code modules} son los registrados en {@code team.md}: solo sus especialistas son artefactos de la aplicación. */
    public Inventory scanProject(Path project, ProviderType provider, Collection<String> modules) {
        return scan(paths.projectBase(provider, project), ArtifactScope.PROJECT, provider, modules);
    }

    private Inventory scan(Path root, ArtifactScope scope, ProviderType provider, Collection<String> modules) {
        List<InstalledArtifact> installed = new ArrayList<>();
        List<Inventory.Problem> problems = new ArrayList<>();

        if(!Files.isDirectory(root))
            return new Inventory(installed, problems);

        for(CatalogEntry entry : catalog.entries(provider, scope, modules)) {
            Path path = root.resolve(entry.location());
            if(!Files.exists(path))
                continue;

            try {
                installed.add(inspect(entry, root));
            } catch(DomainException e) {
                problems.add(new Inventory.Problem(path, e.code()));
            }
        }
        return new Inventory(installed, problems);
    }

    /**
     * Estado de un único artefacto del catálogo bajo la base del proveedor {@code root} (ver {@code ProviderLayout}).
     * Lanza {@code DomainException} si es ilegible o de un tipo distinto al esperado.
     */
    public InstalledArtifact inspect(CatalogEntry entry, Path root) {
        Path path = root.resolve(entry.location());
        return describe(entry, path, root, new TextStore(paths, List.of(root), backupRoot));
    }

    private InstalledArtifact describe(CatalogEntry entry, Path path, Path root, TextStore text) {
        if(entry.isDirectory() != Files.isDirectory(path))
            throw new DomainException(AppErrorCode.ARTIFACT_KIND_MISMATCH);

        String hash = entry.isDirectory() ? directoryHash(path, root) : ContentHash.of(text.read(path).content());
        Optional<ArtifactRecord> record = metadata.artifact(path);

        ArtifactStatus status = record
            .map(r -> r.state() == LinkState.DETACHED ? ArtifactStatus.DETACHED : ArtifactStatus.LINKED)
            .orElse(ArtifactStatus.LEGACY);
        boolean modified = record.isPresent() && !record.get().hash().equals(hash);

        return new InstalledArtifact(entry, path, hash, status, modified);
    }

    // Una skill es un directorio completo: SKILL.md y todos sus ficheros de apoyo, también binarios
    private String directoryHash(Path dir, Path root) {
        Map<String, String> hashes = new TreeMap<>();
        try(Stream<Path> files = Files.walk(dir)) {
            for(Path file : files.filter(Files::isRegularFile).toList()) {
                paths.requireWithin(root, file);
                hashes.put(dir.relativize(file).toString(), fileHash(Files.readAllBytes(file)));
            }
        } catch(IOException e) {
            throw new DomainException(AppErrorCode.TEXT_READ_FAILED, e);
        }
        return ContentHash.ofFiles(hashes);
    }

    private static String fileHash(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
            return ContentHash.of(text);
        } catch(CharacterCodingException e) {
            return ContentHash.ofBytes(bytes);
        }
    }
}
