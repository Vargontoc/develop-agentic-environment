package es.vargontoc.agentic.develop.cli.application;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.domain.artifact.InstalledArtifact;
import es.vargontoc.agentic.develop.cli.domain.diff.TextDiff;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactParts;
import es.vargontoc.agentic.develop.cli.domain.metadata.ArtifactRecord;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

/**
 * Guarda el cuerpo de una única instancia de artefacto. La cabecera se conserva tal cual. Antes de escribir compara el
 * hash actual con el registrado: si el artefacto cambió fuera de la aplicación devuelve un conflicto, salvo que se pida
 * sobrescribir de forma explícita (en ese caso queda una copia del contenido anterior). Un guardado sin diferencias no
 * escribe ningún fichero.
 */
public class ArtifactSaver {

    /**
     * @param written  falso si no había diferencias y no se escribió nada
     * @param backup   copia del contenido anterior, o null si no se escribió
     * @param tracked  verdadero si el artefacto tiene registro y su hash quedó actualizado
     * @param diff     líneas cambiadas con prefijo {@code -} y {@code +}; vacío si no hay cambios
     */
    public record SaveResult(Path path, boolean written, Path backup, boolean tracked, String diff) {}

    /**
     * Resultado de guardar ficheros de una skill.
     *
     * @param diffs   diff de cada fichero escrito, por su ruta relativa a la skill (los nuevos aparecen todo añadidos)
     * @param backups copias de los ficheros que ya existían y se sobrescribieron
     */
    public record SkillSaveResult(Path path, boolean written, List<Path> backups, boolean tracked, Map<String, String> diffs) {}

    private final PathResolver paths;
    private final ArtifactInventory inventory;
    private final MetadataStore metadata;
    private final Path backupRoot;

    public ArtifactSaver(PathResolver paths, ArtifactInventory inventory, MetadataStore metadata, Path backupRoot) {
        this.paths = paths;
        this.inventory = inventory;
        this.metadata = metadata;
        this.backupRoot = backupRoot;
    }

    /**
     * @param newBody   nuevo cuerpo lógico (sin cabecera); en un artefacto sin formato es el contenido completo
     * @param overwrite confirma que se sobrescriba aunque haya cambios externos
     */
    public SaveResult saveInstance(ArtifactLocation location, String newBody, boolean overwrite) {
        CatalogEntry entry = location.entry();
        Path path = location.path();
        try {
            if(entry.isDirectory())
                throw new DomainException(AppErrorCode.ARTIFACT_KIND_MISMATCH);

            InstalledArtifact installed = inventory.inspect(entry, location.root());
            if(installed.modifiedExternally() && !overwrite)
                throw new DomainException(AppErrorCode.ARTIFACT_CONFLICT);

            TextStore text = new TextStore(paths, List.of(location.root()), backupRoot);
            String current = text.read(path).content();
            String composed = entry.format() == null ? newBody : compose(entry, current, newBody);

            TextStore.WriteResult result = text.write(path, composed);
            String diff = result.written() ? TextDiff.between(current, composed) : "";

            boolean tracked = syncRecord(path, result.written() || overwrite);
            return new SaveResult(path, result.written(), result.backup(), tracked, diff);
        } catch(PathDomainException e) {
            throw e;
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), path, e);
        }
    }

    /**
     * Guarda ficheros de texto de una skill. Solo escribe los indicados: el resto de la skill (otros textos y binarios)
     * no se toca ni se borra. Las rutas son relativas a la skill y no pueden salir de ella.
     *
     * @param files     ruta relativa a la skill (con {@code /}) y su nuevo contenido
     * @param overwrite confirma que se sobrescriba aunque la skill tenga cambios externos
     */
    public SkillSaveResult saveSkillFiles(ArtifactLocation location, Map<String, String> files, boolean overwrite) {
        CatalogEntry entry = location.entry();
        Path skill = location.path();
        try {
            if(!entry.isDirectory())
                throw new DomainException(AppErrorCode.ARTIFACT_KIND_MISMATCH);

            InstalledArtifact installed = inventory.inspect(entry, location.root());
            if(installed.modifiedExternally() && !overwrite)
                throw new DomainException(AppErrorCode.ARTIFACT_CONFLICT);

            TextStore text = new TextStore(paths, List.of(skill), backupRoot);
            Map<Path, String> targets = new TreeMap<>();
            Map<Path, String> names = new LinkedHashMap<>();
            for(Map.Entry<String, String> file : new TreeMap<>(files).entrySet()) {
                Path target = fileInside(skill, file.getKey());
                if(Files.isDirectory(target))
                    throw new DomainException(AppErrorCode.ARTIFACT_KIND_MISMATCH);
                targets.put(target, file.getValue());
                names.put(target, file.getKey());
                if(Files.exists(target))
                    text.read(target);
            }

            Map<String, String> diffs = new TreeMap<>();
            List<Path> backups = new ArrayList<>();
            for(Map.Entry<Path, String> target : targets.entrySet()) {
                String current = Files.exists(target.getKey()) ? text.read(target.getKey()).content() : "";
                TextStore.WriteResult result = text.write(target.getKey(), target.getValue());
                if(!result.written())
                    continue;
                diffs.put(names.get(target.getKey()), TextDiff.between(current, target.getValue()));
                if(result.backup() != null)
                    backups.add(result.backup());
            }

            boolean written = !diffs.isEmpty();
            boolean tracked = syncDirectoryRecord(location, written || overwrite);
            return new SkillSaveResult(skill, written, List.copyOf(backups), tracked, diffs);
        } catch(PathDomainException e) {
            throw e;
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), skill, e);
        }
    }

    // La ruta debe ser relativa, sin salirse de la skill ni usar enlaces que apunten fuera
    private Path fileInside(Path skill, String relative) {
        if(relative == null || relative.isBlank())
            throw new DomainException(AppErrorCode.PATH_OUT_OF_SCOPE);
        try {
            Path target = skill.resolve(relative.replace('\\', '/')).normalize();
            if(Path.of(relative).isAbsolute() || !target.startsWith(skill) || target.equals(skill))
                throw new DomainException(AppErrorCode.PATH_OUT_OF_SCOPE);
            paths.requireWithin(skill, target);
            return target;
        } catch(InvalidPathException e) {
            throw new DomainException(AppErrorCode.PATH_OUT_OF_SCOPE, e);
        }
    }

    private boolean syncDirectoryRecord(ArtifactLocation location, boolean update) {
        var record = metadata.artifact(location.path());
        if(record.isEmpty())
            return false;
        if(update) {
            ArtifactRecord r = record.get();
            metadata.putArtifactHash(location.path(), r.groupId(), inventory.inspect(location.entry(), location.root()).hash(), r.state());
        }
        return true;
    }

    private static String compose(CatalogEntry entry, String current, String newBody) {
        ArtifactParts parts = entry.format().split(current);
        return entry.format().compose(parts.withBody(newBody));
    }

    // El registro sigue al contenido en disco; un artefacto legacy no tiene registro y se queda como está
    private boolean syncRecord(Path path, boolean update) {
        var record = metadata.artifact(path);
        if(record.isEmpty())
            return false;
        if(update) {
            ArtifactRecord r = record.get();
            String content = new TextStore(paths, List.of(path.getParent()), backupRoot).read(path).content();
            metadata.putArtifact(path, r.groupId(), content, r.state());
        }
        return true;
    }
}
