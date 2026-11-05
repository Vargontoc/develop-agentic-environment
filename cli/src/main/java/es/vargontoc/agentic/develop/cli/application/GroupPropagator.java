package es.vargontoc.agentic.develop.cli.application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.format.BodyPlaceholders;
import es.vargontoc.agentic.develop.cli.domain.metadata.ArtifactRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;

/**
 * Propaga un cuerpo compartido a los artefactos que ya existen de un mismo grupo (DEC-006). Nunca crea artefactos
 * ausentes ni cruza entre el ámbito global y el de proyecto. En los especialistas de proyecto el cuerpo compartido
 * lleva placeholders ({@link BodyPlaceholders}) que se resuelven con el módulo y el proveedor de cada artefacto; la cabecera de
 * cada artefacto no se toca (DEC-007). Los desvinculados se omiten siempre y los modificados fuera de la aplicación
 * se omiten salvo que se pida sobrescribir de forma explícita.
 */
public class GroupPropagator {

    public enum Outcome {
        UPDATED, UNCHANGED, CONFLICT, DETACHED, CUSTOM, MISSING, OUT_OF_SCOPE, FAILED
    }

    /**
     * @param diff    líneas cambiadas con prefijo {@code -} y {@code +}; vacío si no se escribió. En una skill, cada
     *                fichero escrito va precedido de una línea {@code @@ ruta}
     * @param backups copias de lo que se sobrescribió; vacío si no se escribió nada
     */
    public record Member(Path path, ProviderType provider, String module, Outcome outcome, String diff, List<Path> backups) {}

    /** @param groupUpdated verdadero si el cuerpo compartido guardado en el grupo cambió */
    public record Result(String groupId, List<Member> members, boolean groupUpdated) {

        public boolean changed() {
            return members.stream().anyMatch(m -> m.outcome() == Outcome.UPDATED);
        }
    }

    private final MetadataStore metadata;
    private final ArtifactDescriptor descriptor;
    private final ArtifactSaver saver;
    private final MigrationService migration;

    public GroupPropagator(MetadataStore metadata, ArtifactDescriptor descriptor, ArtifactSaver saver, MigrationService migration) {
        this.migration = migration;
        this.metadata = metadata;
        this.descriptor = descriptor;
        this.saver = saver;
    }

    /**
     * @param project   proyecto cuyos artefactos se propagan; null si el grupo es global
     * @param overwrite sobrescribe también los artefactos con cambios externos (se conserva copia de cada uno)
     */
    public Result propagate(String groupId, String sharedBody, Path project, boolean overwrite) {
        GroupRecord group = metadata.group(groupId)
            .orElseThrow(() -> new PathDomainException(AppErrorCode.GROUP_NOT_FOUND, groupId));

        List<Member> members = members(group, project, location -> {
            String body = BodyPlaceholders.resolve(sharedBody, location.provider(), location.entry().module());
            ArtifactSaver.SaveResult saved = saver.saveInstance(location, body, overwrite);
            return new Written(saved.written(), saved.diff(), saved.backup() == null ? List.of() : List.of(saved.backup()));
        });

        boolean groupUpdated = !sharedBody.equals(group.sharedBody());
        if(groupUpdated)
            metadata.putGroup(new GroupRecord(group.id(), group.scope(), sharedBody));
        return new Result(groupId, members, groupUpdated);
    }

    /**
     * Propaga ficheros de una skill a las copias existentes del grupo. Solo escribe los ficheros indicados: los demás de
     * cada copia, como los auxiliares propios de un proveedor, se conservan. No cambia el cuerpo compartido del grupo.
     *
     * @param files ruta relativa a la skill (con {@code /}) y su nuevo contenido
     */
    public Result propagateFiles(String groupId, Map<String, String> files, Path project, boolean overwrite) {
        GroupRecord group = metadata.group(groupId)
            .orElseThrow(() -> new PathDomainException(AppErrorCode.GROUP_NOT_FOUND, groupId));

        List<Member> members = members(group, project, location -> {
            ArtifactSaver.SkillSaveResult saved = saver.saveSkillFiles(location, files, overwrite);
            StringBuilder diff = new StringBuilder();
            new TreeMap<>(saved.diffs()).forEach((file, d) -> {
                if(!diff.isEmpty())
                    diff.append('\n');
                diff.append("@@ ").append(file);
                if(!d.isEmpty())
                    diff.append('\n').append(d);
            });
            return new Written(saved.written(), diff.toString(), saved.backups());
        });
        return new Result(groupId, members, false);
    }

    private record Written(boolean written, String diff, List<Path> backups) {}

    private List<Member> members(GroupRecord group, Path project, Function<ArtifactLocation, Written> write) {
        List<Member> members = new java.util.ArrayList<>(metadata.artifactsOfGroup(group.id()).stream()
            .sorted(Comparator.comparing(ArtifactRecord::path))
            .map(r -> propagateTo(group, r, project, write))
            .toList());
        // Los legacy son personalizados (DEC-013): no se tocan, solo se informan
        migration.legacyOf(group, project).forEach(l ->
            members.add(skipped(l.path(), l.provider(), l.entry().module(), Outcome.CUSTOM)));
        return members;
    }

    private Member propagateTo(GroupRecord group, ArtifactRecord record, Path project, Function<ArtifactLocation, Written> write) {
        Path path = Path.of(record.path());
        if(!Files.exists(path))
            return skipped(path, null, null, Outcome.MISSING);
        if(record.state() == LinkState.DETACHED)
            return skipped(path, null, null, Outcome.DETACHED);

        ArtifactLocation location;
        try {
            location = descriptor.resolve(path.toString(), project);
        } catch(DomainException e) {
            return skipped(path, null, null, e.code() == AppErrorCode.ARTIFACT_NOT_CATALOGUED ? Outcome.OUT_OF_SCOPE : Outcome.FAILED);
        }
        if(location.scope() != group.scope())
            return skipped(path, location.provider(), location.entry().module(), Outcome.OUT_OF_SCOPE);

        String module = location.entry().module();
        try {
            Written saved = write.apply(location);
            return new Member(path, location.provider(), module, saved.written() ? Outcome.UPDATED : Outcome.UNCHANGED,
                saved.diff(), saved.backups());
        } catch(DomainException e) {
            Outcome outcome = e.code() == AppErrorCode.ARTIFACT_CONFLICT ? Outcome.CONFLICT : Outcome.FAILED;
            return skipped(path, location.provider(), module, outcome);
        }
    }

    private static Member skipped(Path path, ProviderType provider, String module, Outcome outcome) {
        return new Member(path, provider, module, outcome, "", List.of());
    }
}
