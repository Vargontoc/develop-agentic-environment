package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactCatalog;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactDescription;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.domain.artifact.InstalledArtifact;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactParts;
import es.vargontoc.agentic.develop.cli.domain.metadata.ArtifactRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;

/**
 * Describe un artefacto instalado de la aplicación: cabecera, cuerpo, cuerpo compartido, formato, proveedor, módulo,
 * hash y estado de vinculación. Solo describe lo que está en disco y figura en el catálogo; nunca lo del jar.
 * Todos los errores llevan la ruta afectada ({@link PathDomainException}).
 */
public class ArtifactDescriptor {

    private record Located(ProviderType provider, ArtifactScope scope, Path root, CatalogEntry entry) {}

    private final PathResolver paths;
    private final ArtifactCatalog catalog;
    private final ArtifactInventory inventory;
    private final MetadataStore metadata;
    private final ProjectWorkspace workspace;
    private final Path backupRoot;

    public ArtifactDescriptor(PathResolver paths, ArtifactCatalog catalog, ArtifactInventory inventory,
                              MetadataStore metadata, ProjectWorkspace workspace, Path backupRoot) {
        this.paths = paths;
        this.catalog = catalog;
        this.inventory = inventory;
        this.metadata = metadata;
        this.workspace = workspace;
        this.backupRoot = backupRoot;
    }

    /**
     * @param input   ruta del artefacto tal como la escribe el usuario; en una skill vale el directorio o un fichero suyo
     * @param project proyecto activo, o null si solo se describen artefactos globales
     */
    public ArtifactDescription describe(String input, Path project) {
        Path path = locate(input);
        Located located = find(path, project);

        try {
            InstalledArtifact installed = inventory.inspect(located.entry(), located.root());
            return build(located, installed);
        } catch(PathDomainException e) {
            throw e;
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), path, e);
        }
    }

    // Una ruta que solo existe como recurso del jar no es un artefacto editable
    private Path locate(String input) {
        String raw = input == null ? "" : input.trim();
        if(raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\""))
            raw = raw.substring(1, raw.length() - 1).trim();

        if(raw.startsWith("jar:") || raw.startsWith("classpath:") || raw.contains(".jar!"))
            throw new PathDomainException(AppErrorCode.ARTIFACT_NOT_EDITABLE, raw);

        Path path;
        try {
            path = Path.of(raw).toAbsolutePath().normalize();
        } catch(InvalidPathException e) {
            throw new PathDomainException(AppErrorCode.PROJECT_PATH_INVALID, raw);
        }

        if(raw.isEmpty() || !Files.exists(path)) {
            String resource = raw.replace('\\', '/');
            boolean inJar = !raw.isEmpty()
                && (ClasspathResources.exists(resource) || ClasspathResources.exists("/" + resource));
            throw new PathDomainException(inJar ? AppErrorCode.ARTIFACT_NOT_EDITABLE : AppErrorCode.ARTIFACT_NOT_FOUND, raw);
        }
        return path;
    }

    private Located find(Path path, Path project) {
        if(paths.hasHome()) {
            for(ProviderType provider : ProviderType.values()) {
                Optional<Located> found = match(path, provider, ArtifactScope.GLOBAL, paths.providerHome(provider), List.of());
                if(found.isPresent())
                    return found.get();
            }
        }

        if(project != null) {
            List<String> modules = workspace.readTeam(project).moduleNames();
            for(ProviderType provider : ProviderType.values()) {
                if(!provider.supportsProject())
                    continue;
                Optional<Located> found = match(path, provider, ArtifactScope.PROJECT,
                    project.resolve(provider.projectDir()), modules);
                if(found.isPresent())
                    return found.get();
            }
        }
        throw new PathDomainException(AppErrorCode.ARTIFACT_NOT_CATALOGUED, path);
    }

    private Optional<Located> match(Path path, ProviderType provider, ArtifactScope scope, Path root, List<String> modules) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if(!path.startsWith(normalizedRoot) || path.equals(normalizedRoot))
            return Optional.empty();

        String relative = normalizedRoot.relativize(path).toString().replace('\\', '/');
        Optional<CatalogEntry> entry = catalog.identify(provider, scope, relative, modules)
            .or(() -> catalog.identify(provider, scope, skillRoot(relative), modules));
        if(entry.isEmpty())
            return Optional.empty();

        try {
            paths.requireWithin(normalizedRoot, path);
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), path, e);
        }
        return Optional.of(new Located(provider, scope, normalizedRoot, entry.get()));
    }

    // Un fichero dentro de una skill (SKILL.md, assets) describe la skill entera
    private static String skillRoot(String relative) {
        String[] parts = relative.split("/");
        return parts.length > 2 && "skills".equals(parts[0]) ? parts[0] + "/" + parts[1] : relative;
    }

    private ArtifactDescription build(Located located, InstalledArtifact installed) {
        CatalogEntry entry = located.entry();
        Path artifact = installed.path();
        Optional<ArtifactRecord> record = metadata.artifact(artifact);
        String sharedBody = record.flatMap(r -> metadata.group(r.groupId())).map(GroupRecord::sharedBody).orElse(null);

        String header = "";
        String body = "";
        List<String> files = List.of();

        if(entry.isDirectory()) {
            files = skillFiles(artifact);
        } else {
            String content = new TextStore(paths, List.of(located.root()), backupRoot).read(artifact).content();
            if(entry.format() == null) {
                body = content;
            } else {
                ArtifactParts parts = entry.format().split(content);
                header = parts.header();
                body = parts.body();
            }
        }

        return new ArtifactDescription(located.provider(), located.scope(), entry.kind(), entry.name(), artifact,
            entry.format(), entry.module(), header, body, sharedBody, installed.hash(),
            installed.status(), installed.modifiedExternally(), files);
    }

    private List<String> skillFiles(Path dir) {
        try(Stream<Path> files = Files.walk(dir)) {
            return files.filter(Files::isRegularFile)
                .map(f -> dir.relativize(f).toString().replace('\\', '/'))
                .sorted()
                .toList();
        } catch(IOException e) {
            throw new PathDomainException(AppErrorCode.TEXT_READ_FAILED, dir, e);
        }
    }
}
