package es.vargontoc.agentic.develop.cli.application;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactStatus;
import es.vargontoc.agentic.develop.cli.domain.artifact.InstalledArtifact;
import es.vargontoc.agentic.develop.cli.domain.diff.TextDiff;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.format.BodyPlaceholders;
import es.vargontoc.agentic.develop.cli.domain.metadata.ArtifactRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupIds;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

/**
 * Artefactos que existían antes de que hubiera metadatos de agrupación (DEC-013). Un artefacto sin registro es
 * legacy: se trata como personalizado, la propagación no lo toca y solo entra en un grupo si el usuario adopta
 * explícitamente el cuerpo compartido. También permite desvincular un artefacto registrado.
 */
public class MigrationService {

    /**
     * Resultado de adoptar un cuerpo compartido.
     *
     * @param applied falso si solo se calculó la vista previa (sin confirmar) o si no había diferencias
     * @param diff    cambios que se aplicarían, o que se aplicaron
     * @param backup  copia del contenido anterior; null si no se escribió
     */
    public record Adoption(Path path, String groupId, boolean applied, String diff, Path backup) {}

    private final PathResolver paths;
    private final ArtifactInventory inventory;
    private final ProjectWorkspace workspace;
    private final MetadataStore metadata;
    private final ArtifactSaver saver;

    public MigrationService(PathResolver paths, ArtifactInventory inventory, ProjectWorkspace workspace,
                            MetadataStore metadata, ArtifactSaver saver) {
        this.paths = paths;
        this.inventory = inventory;
        this.workspace = workspace;
        this.metadata = metadata;
        this.saver = saver;
    }

    /** Artefactos legacy instalados que pertenecerían al grupo, en el ámbito del grupo; solo informativo. */
    public List<ArtifactLocation> legacyOf(GroupRecord group, Path project) {
        List<ArtifactLocation> found = new ArrayList<>();
        if(group.scope() == ArtifactScope.GLOBAL) {
            if(paths.hasHome())
                for(ProviderType provider : ProviderType.values())
                    collect(inventory.scanGlobal(provider).artifacts(), provider, ArtifactScope.GLOBAL,
                        paths.globalBase(provider), group, found);
        } else if(project != null) {
            List<String> modules = workspace.readTeam(project).moduleNames();
            for(ProviderType provider : ProviderType.values())
                collect(inventory.scanProject(project, provider, modules).artifacts(), provider, ArtifactScope.PROJECT,
                    paths.projectBase(provider, project), group, found);
        }
        found.sort(Comparator.comparing(l -> l.path().toString()));
        return found;
    }

    private void collect(List<InstalledArtifact> installed, ProviderType provider, ArtifactScope scope, Path root,
                         GroupRecord group, List<ArtifactLocation> out) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        for(InstalledArtifact artifact : installed)
            if(artifact.status() == ArtifactStatus.LEGACY && GroupIds.of(artifact.entry()).equals(group.id()))
                out.add(new ArtifactLocation(provider, scope, normalizedRoot, artifact.entry()));
    }

    /**
     * Adopta el cuerpo compartido del grupo en un agente legacy: lo sustituye por el cuerpo del grupo (con el módulo
     * del artefacto) y lo registra como vinculado. Sin {@code confirm} solo devuelve el diff.
     */
    public Adoption adopt(ArtifactLocation location, String groupId, boolean confirm) {
        GroupRecord group = group(groupId);
        Path path = location.path();
        requireAdoptable(location);

        String body = BodyPlaceholders.resolve(group.sharedBody(), location.provider(), location.entry().module());

        if(!confirm) {
            String current = saver.currentBody(location);
            return new Adoption(path, groupId, false, TextDiff.between(current, body), null);
        }

        ArtifactSaver.SaveResult saved = saver.saveInstance(location, body, true);
        register(location, groupId, group);
        return new Adoption(path, groupId, saved.written(), saved.diff(), saved.backup());
    }

    /** Como {@link #adopt} para una skill: adopta los ficheros indicados, sin tocar el resto de la skill. */
    public Adoption adoptSkill(ArtifactLocation location, String groupId, Map<String, String> files, boolean confirm) {
        GroupRecord group = groupForSkill(location, groupId);
        requireAdoptable(location);

        if(!confirm) {
            StringBuilder preview = new StringBuilder();
            for(var file : new TreeMap<>(files).entrySet()) {
                String current = saver.currentFile(location, file.getKey());
                if(!preview.isEmpty())
                    preview.append('\n');
                preview.append("@@ ").append(file.getKey());
                String diff = TextDiff.between(current, file.getValue());
                if(!diff.isEmpty())
                    preview.append('\n').append(diff);
            }
            return new Adoption(location.path(), groupId, false, preview.toString(), null);
        }

        ArtifactSaver.SkillSaveResult saved = saver.saveSkillFiles(location, files, true);
        register(location, groupId, group);
        StringBuilder diff = new StringBuilder();
        new TreeMap<>(saved.diffs()).forEach((file, d) -> {
            if(!diff.isEmpty())
                diff.append('\n');
            diff.append("@@ ").append(file);
            if(!d.isEmpty())
                diff.append('\n').append(d);
        });
        return new Adoption(location.path(), groupId, saved.written(), diff.toString(),
            saved.backups().isEmpty() ? null : saved.backups().get(0));
    }

    /**
     * Desvincula un artefacto registrado: deja de recibir propagaciones. Un artefacto legacy ya es personalizado, así
     * que no cambia nada.
     *
     * @return verdadero si el estado cambió
     */
    public boolean detach(ArtifactLocation location) {
        Optional<ArtifactRecord> record = metadata.artifact(location.path());
        if(record.isEmpty() || record.get().state() == LinkState.DETACHED)
            return false;
        metadata.markDetached(location.path());
        return true;
    }

    private GroupRecord group(String groupId) {
        return metadata.group(groupId).orElseThrow(() -> new PathDomainException(AppErrorCode.GROUP_NOT_FOUND, groupId));
    }

    // Una skill no tiene cuerpo compartido: su grupo puede crearse al adoptarla si nadie lo había registrado aún
    private GroupRecord groupForSkill(ArtifactLocation location, String groupId) {
        Optional<GroupRecord> existing = metadata.group(groupId);
        if(existing.isPresent())
            return existing.get();
        if(!groupId.equals(GroupIds.of(location.entry())))
            throw new PathDomainException(AppErrorCode.GROUP_NOT_FOUND, groupId);
        return new GroupRecord(groupId, location.scope(), "");
    }

    // Se puede adoptar lo que no tiene registro (legacy) y lo desvinculado (para volver al grupo); no lo ya vinculado
    private void requireAdoptable(ArtifactLocation location) {
        Optional<ArtifactRecord> record = metadata.artifact(location.path());
        if(record.isPresent() && record.get().state() == LinkState.LINKED)
            throw new PathDomainException(AppErrorCode.ARTIFACT_NOT_LEGACY, location.path());
    }

    // Tras adoptar, el artefacto queda registrado con el hash de lo que hay en disco
    private void register(ArtifactLocation location, String groupId, GroupRecord group) {
        if(metadata.group(groupId).isEmpty())
            metadata.putGroup(group);
        InstalledArtifact now = inventory.inspect(location.entry(), location.root());
        metadata.putArtifactHash(location.path(), groupId, now.hash(), LinkState.LINKED);
    }
}
