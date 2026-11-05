package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactCatalog;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.format.BodyPlaceholders;
import es.vargontoc.agentic.develop.cli.domain.format.HeaderMarkers;
import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupIds;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;

/**
 * Crea un agente o una skill del catálogo a partir de su plantilla, tal cual o con un contenido propio, por la misma
 * pipeline que la configuración (catálogo, cabecera por sistema, formato y {@code TextStore}). Si ya existe, no se
 * sobrescribe sin confirmación explícita (queda entonces una copia del contenido anterior). El artefacto queda
 * registrado en los metadatos con su grupo y su hash.
 * <p>
 * El contenido de un agente es su cuerpo lógico (la cabecera la pone la plantilla); el de una skill es su
 * {@code SKILL.md}, y el resto de ficheros de la skill vienen de la plantilla.
 */
public class CreateArtifact {

    private static final String HELPER = "/global/helper/openai.yaml";

    /**
     * @param replaced verdadero si existía y se sobrescribió
     * @param backup   copia del contenido anterior, o null
     * @param custom   verdadero si el contenido no es el de la plantilla
     */
    public record Created(Path path, ArtifactKind kind, boolean replaced, Path backup, boolean custom) {}

    private final TargetOs os;
    private final PathResolver paths;
    private final ArtifactCatalog catalog;
    private final ArtifactInventory inventory;
    private final MetadataStore metadata;
    private final ProjectWorkspace workspace;
    private final Path backupRoot;

    public CreateArtifact(TargetOs os, PathResolver paths, ArtifactCatalog catalog, ArtifactInventory inventory,
                          MetadataStore metadata, ProjectWorkspace workspace, Path backupRoot) {
        this.os = os;
        this.paths = paths;
        this.catalog = catalog;
        this.inventory = inventory;
        this.metadata = metadata;
        this.workspace = workspace;
        this.backupRoot = backupRoot;
    }

    /**
     * @param content   cuerpo del agente o {@code SKILL.md} de la skill; null para usar la plantilla tal cual
     * @param overwrite confirma que se sobrescriba un artefacto existente
     * @param project   proyecto activo; solo se usa en el ámbito de proyecto
     */
    public Created execute(ProviderType type, ArtifactScope scope, ArtifactKind kind, String name, Path project,
                           String content, boolean overwrite) {
        Path root = rootOf(type, scope, project, name);
        CatalogEntry entry = entryOf(type, scope, kind, name, project);
        Path target = root.resolve(entry.relativePath());

        boolean exists = Files.exists(target);
        if(exists && !overwrite)
            throw new PathDomainException(AppErrorCode.ARTIFACT_EXISTS, target);

        try {
            // La carpeta del proveedor puede no existir aún; la comprobación de ámbito necesita una ruta real
            Files.createDirectories(root);
            paths.requireWithin(root, target);
            TextStore text = new TextStore(paths, List.of(root), backupRoot);
            Path backup;
            String shared;
            if(entry.isDirectory()) {
                backup = writeSkill(entry, target, exists, content, text);
                shared = "";
            } else {
                shared = agentBody(entry, content);
                String rendered = HeaderMarkers.render(
                    ClasspathResources.readText(entry.resource()).orElseThrow(() -> missing(entry.resource())), os, type);
                // Los placeholders solo se resuelven en proyecto y en el contenido de la plantilla (el router no tiene módulo)
                boolean inProject = scope == ArtifactScope.PROJECT;
                String header = inProject ? BodyPlaceholders.resolve(rendered, type, entry.module()) : rendered;
                String body = content != null || !inProject ? shared : BodyPlaceholders.resolve(shared, type, entry.module());
                backup = text.write(target, entry.format().assemble(header, body)).backup();
            }

            register(entry, root, shared);
            return new Created(target, kind, exists, backup, content != null);
        } catch(IOException e) {
            throw new PathDomainException(AppErrorCode.MODULE_WRITE_FAILED, target, e);
        } catch(PathDomainException e) {
            throw e;
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), target, e);
        }
    }

    private Path rootOf(ProviderType type, ArtifactScope scope, Path project, String name) {
        if(scope == ArtifactScope.GLOBAL) {
            if(!paths.hasHome())
                throw new PathDomainException(AppErrorCode.ARTIFACT_SCOPE_UNSUPPORTED, name);
            return paths.providerHome(type).toAbsolutePath().normalize();
        }
        if(!type.supportsProject())
            throw new PathDomainException(AppErrorCode.ARTIFACT_SCOPE_UNSUPPORTED, type.id());
        if(project == null)
            throw new DomainException(AppErrorCode.MODULE_NO_PROJECT);
        if(!Files.exists(project))
            throw new PathDomainException(AppErrorCode.PROJECT_PATH_NOT_FOUND, project);
        if(!Files.isDirectory(project))
            throw new PathDomainException(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY, project);
        return project.resolve(type.projectDir()).toAbsolutePath().normalize();
    }

    private CatalogEntry entryOf(ProviderType type, ArtifactScope scope, ArtifactKind kind, String name, Path project) {
        List<String> modules = scope == ArtifactScope.PROJECT ? workspace.readTeam(project).moduleNames() : List.of();
        return catalog.entries(type, scope, modules).stream()
            .filter(e -> e.kind() == kind && e.name().equals(name))
            .findFirst()
            .orElseThrow(() -> new PathDomainException(AppErrorCode.ARTIFACT_NOT_CATALOGUED, name));
    }

    // Contenido propio, o el cuerpo ya compartido por el grupo, o la plantilla
    private String agentBody(CatalogEntry entry, String content) throws IOException {
        if(content != null)
            return content;

        Optional<String> grouped = metadata.group(GroupIds.of(entry)).map(GroupRecord::sharedBody).filter(b -> !b.isBlank());
        if(grouped.isPresent())
            return grouped.get();

        String template = ClasspathResources.readText(entry.bodyResource()).orElseThrow(() -> missing(entry.bodyResource()));
        return entry.scope() == ArtifactScope.PROJECT ? template.strip() + "\n" : template;
    }

    // Skill nueva: copia de la plantilla y, si hay contenido, su SKILL.md. Existente: solo se rehace SKILL.md
    private Path writeSkill(CatalogEntry entry, Path target, boolean exists, String content, TextStore text) throws IOException {
        if(!exists) {
            ClasspathResources.copyTreeIfAbsent(entry.resource(), target)
                .orElseThrow(() -> missing(entry.resource()));
            if(entry.provider() == ProviderType.CODEX && entry.scope() == ArtifactScope.GLOBAL)
                copyHelper(target);
        }
        if(content == null && !exists)
            return null;

        String skillMd = content != null ? content
            : ClasspathResources.readText(entry.resource() + "/SKILL.md").orElseThrow(() -> missing(entry.resource() + "/SKILL.md"));
        Path backup = text.write(target.resolve("SKILL.md"), skillMd).backup();
        if(exists)
            ClasspathResources.copyTreeIfAbsent(entry.resource(), target);
        return backup;
    }

    private static void copyHelper(Path skill) throws IOException {
        Path helper = skill.resolve("agents/openai.yaml");
        if(Files.exists(helper))
            return;
        String yaml = ClasspathResources.readText(HELPER).orElseThrow(() -> missing(HELPER));
        Files.createDirectories(helper.getParent());
        Files.writeString(helper, yaml);
    }

    private static DomainException missing(String resource) {
        return new PathDomainException(AppErrorCode.RESOURCE_READ_FAILED, resource);
    }

    // El grupo se crea si no existía; el artefacto queda vinculado con el hash de lo escrito
    private void register(CatalogEntry entry, Path root, String shared) {
        String groupId = GroupIds.of(entry);
        if(metadata.group(groupId).isEmpty())
            metadata.putGroup(new GroupRecord(groupId, entry.scope(), shared));
        metadata.putArtifactHash(root.resolve(entry.relativePath()), groupId, inventory.inspect(entry, root).hash(), LinkState.LINKED);
    }
}
