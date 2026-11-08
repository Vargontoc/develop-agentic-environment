package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactCatalog;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.domain.artifact.ProviderLayout;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.format.BodyPlaceholders;
import es.vargontoc.agentic.develop.cli.domain.format.HeaderMarkers;
import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
import es.vargontoc.agentic.develop.cli.domain.metadata.ArtifactRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupIds;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.domain.team.TeamInfo;
import es.vargontoc.agentic.develop.cli.domain.team.TeamParser;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;

/**
 * Crea un módulo de proyecto: lo registra en {@code .sdd/team.md} y genera, por técnico y proveedor, su router y su
 * especialista, además de copiar las skills del proyecto. Nada existente se sobrescribe. Cada artefacto nuevo queda
 * registrado en los metadatos con su grupo y su hash. Si el grupo ya tiene un cuerpo compartido, el artefacto nuevo
 * lo usa en lugar de la plantilla. Un fallo al generar un artefacto concreto no detiene el resto: queda como aviso.
 */
public class CreateModule {

    private static final Logger log = LoggerFactory.getLogger(CreateModule.class);

    private static final String SKILLS_RESOURCE = "/project/skills";
    private static final String HELPER = "/global/helper/openai.yaml";
    private static final String CODEX_HELPER_FILE = "agents/openai.yaml";
    private static final String TEAM_FILE = ".sdd/team.md";

    private static final int MIN_NAME_LENGTH = 3;
    private static final Set<String> RESERVED_NAMES = Set.of("router");
    private static final Pattern NAME_CHARS = Pattern.compile("[A-Za-z-]+");

    private final TargetOs os;
    private final PathResolver paths;
    private final ArtifactCatalog catalog;
    private final ArtifactInventory inventory;
    private final MetadataStore metadata;
    private final Path backupRoot;

    /** @param os sistema para el que se generan las cabeceras (DEC-012) */
    public CreateModule(TargetOs os, PathResolver paths, ArtifactCatalog catalog, ArtifactInventory inventory,
                        MetadataStore metadata, Path backupRoot) {
        this.os = os;
        this.paths = paths;
        this.catalog = catalog;
        this.inventory = inventory;
        this.metadata = metadata;
        this.backupRoot = backupRoot;
    }

    /**
     * Solo letras ASCII y guiones simples (nunca al inicio, al final ni seguidos), mínimo 3 caracteres.
     * Devuelve el nombre normalizado a minúsculas.
     */
    public String normalizeName(String raw) {
        String name = raw == null ? "" : raw.trim();

        if(!NAME_CHARS.matcher(name).matches())
            throw new DomainException(AppErrorCode.MODULE_NAME_INVALID_CHARS);
        if(name.startsWith("-") || name.endsWith("-") || name.contains("--"))
            throw new DomainException(AppErrorCode.MODULE_NAME_INVALID_HYPHENS);
        if(name.length() < MIN_NAME_LENGTH)
            throw new DomainException(AppErrorCode.MODULE_NAME_TOO_SHORT);

        String normalized = name.toLowerCase(Locale.ROOT);
        // <rol>-router es el router compartido; un módulo con ese nombre generaría el mismo fichero
        if(RESERVED_NAMES.contains(normalized))
            throw new DomainException(AppErrorCode.MODULE_NAME_RESERVED);

        return normalized;
    }

    /**
     * Valida el nombre y el proyecto antes de crear nada; después registra el módulo y genera sus artefactos.
     * Lanza un error sin haber escrito nada si el nombre o el proyecto no son válidos.
     */
    public OperationResult execute(Path project, String rawModule, List<Technician> technicians, List<Provider> providers) {
        return execute(project, rawModule, technicians, providers, false);
    }

    /**
     * @param dryRun si es verdadero no escribe nada (ni ficheros ni metadatos) y devuelve en {@code created} lo que se
     *               crearía y en {@code skipped} lo que ya existe; las skills se listan una a una
     */
    public OperationResult execute(Path project, String rawModule, List<Technician> technicians, List<Provider> providers,
                                   boolean dryRun) {
        String module = normalizeName(rawModule);
        requireProject(project);

        OperationResult result = new OperationResult();
        TextStore text = new TextStore(paths, List.of(project), backupRoot);

        registerInTeam(project, module, text, result, dryRun);
        generate(project, module, technicians, providers, true, dryRun, text, result);
        return result;
    }

    /**
     * Añade técnicos a un módulo ya registrado en {@code .sdd/team.md}: genera solo los artefactos de esos técnicos
     * (sin skills ni cambios en {@code team.md}) y omite lo que ya existe, de modo que los demás técnicos no cambian.
     * Lanza {@code MODULE_NOT_FOUND} sin escribir nada si el módulo no consta en el equipo.
     */
    public OperationResult addTechnicians(Path project, String rawModule, List<Technician> technicians,
                                          List<Provider> providers) {
        return addTechnicians(project, rawModule, technicians, providers, false);
    }

    /** @param dryRun igual que en {@link #execute(Path, String, List, List, boolean)} */
    public OperationResult addTechnicians(Path project, String rawModule, List<Technician> technicians,
                                          List<Provider> providers, boolean dryRun) {
        String module = normalizeName(rawModule);
        requireProject(project);

        TextStore text = new TextStore(paths, List.of(project), backupRoot);
        Path team = project.resolve(TEAM_FILE);
        if(!Files.exists(team) || !TeamParser.parse(text.read(team).content()).moduleNames().contains(module))
            throw new PathDomainException(AppErrorCode.MODULE_NOT_FOUND, module);

        OperationResult result = new OperationResult();
        generate(project, module, technicians, providers, false, dryRun, text, result);
        return result;
    }

    private static void requireProject(Path project) {
        if(project == null)
            throw new DomainException(AppErrorCode.MODULE_NO_PROJECT);
        if(!Files.exists(project))
            throw new PathDomainException(AppErrorCode.PROJECT_PATH_NOT_FOUND, project);
        if(!Files.isDirectory(project))
            throw new PathDomainException(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY, project);
    }

    private void generate(Path project, String module, List<Technician> technicians, List<Provider> providers,
                          boolean withSkills, boolean dryRun, TextStore text, OperationResult result) {
        List<Created> created = new ArrayList<>();

        for(Provider provider : providers) {
            ProviderType type = provider.getType();
            Path base = paths.projectBase(type, project);
            try {
                paths.requireWithin(project, base);
            } catch(DomainException e) {
                warn(result, type + ": " + base.getFileName() + " queda fuera del proyecto, se omite");
                continue;
            }

            List<CatalogEntry> entries = catalog.entries(type, ArtifactScope.PROJECT, List.of(module));
            if(withSkills)
                copySkills(project, base, type, entries, dryRun, result, created);

            // El router es único por técnico y proveedor; el especialista (y, en Codex, su perfil) depende del módulo
            for(Technician technician : technicians) {
                entries.stream()
                    .filter(e -> e.kind() != ArtifactKind.SKILL && e.technician() == technician)
                    .forEach(e -> generateEntry(new Target(project, base, e, dryRun), text, result, created));
            }
        }

        register(created, result);
    }

    // Artefacto nuevo pendiente de registrar: su entrada, la base de su proveedor y el cuerpo compartido que usó
    private record Created(CatalogEntry entry, Path base, String sharedBody) {}

    private record Target(Path project, Path base, CatalogEntry entry, boolean dryRun) {}

    // Añade "- <modulo> | - | -" al final de la sección "## Módulos" de .sdd/team.md, sin duplicar
    private void registerInTeam(Path project, String module, TextStore text, OperationResult result, boolean dryRun) {
        Path team = project.resolve(TEAM_FILE);
        String label = TEAM_FILE + ": módulo '" + module + "'";

        if(!Files.exists(team)) {
            warn(result, TEAM_FILE + " no existe, el módulo no se ha registrado");
            return;
        }

        try {
            String content = text.read(team).content();
            TeamInfo info = TeamParser.parse(content);
            List<String> lines = new ArrayList<>(List.of(content.split("\\R", -1)));

            int header = lines.indexOf(TeamParser.MODULES_HEADER);
            if(header < 0) {
                warn(result, TEAM_FILE + " no tiene la sección '" + TeamParser.MODULES_HEADER + "', el módulo no se ha registrado");
                return;
            }

            int end = header + 1;
            int lastEntry = -1;
            while(end < lines.size() && !lines.get(end).startsWith("## ")) {
                if(lines.get(end).startsWith("-"))
                    lastEntry = end;
                end++;
            }

            // Se comprueba también con el patrón suelto: una línea mal formada del módulo tampoco debe duplicarse
            Pattern existing = Pattern.compile("-\\s*" + Pattern.quote(module) + "\\s*\\|.*");
            boolean registered = info.moduleNames().contains(module)
                || lines.subList(header + 1, end).stream().anyMatch(l -> existing.matcher(l.trim()).matches());
            if(registered) {
                result.skipped().add(label + " (ya registrado)");
                return;
            }

            String entry = "- " + module + " | - | -";
            if(lastEntry >= 0) {
                lines.add(lastEntry + 1, entry);
            } else {
                // Sin módulos: tras la línea en blanco que sigue al encabezado, dejando otra antes de la siguiente sección
                int at = header + 1;
                if(at < lines.size() && lines.get(at).isBlank())
                    at++;
                lines.add(at, entry);
                if(at + 1 < lines.size() && !lines.get(at + 1).isBlank())
                    lines.add(at + 1, "");
            }

            if(!dryRun)
                text.write(team, String.join("\n", lines));
            result.created().add(label + " registrado");
        } catch(DomainException e) {
            fail(result, TEAM_FILE + ": no se pudo actualizar el registro del módulo", e);
        }
    }

    // Copia solo lo que falta; las skills cuyo directorio no existía antes se registran como nuevas
    private void copySkills(Path project, Path base, ProviderType type, List<CatalogEntry> entries, boolean dryRun,
                            OperationResult result, List<Created> created) {
        List<CatalogEntry> skills = entries.stream().filter(e -> e.kind() == ArtifactKind.SKILL).toList();
        if(skills.isEmpty()) {
            log.error("Resource '{}' not found", SKILLS_RESOURCE);
            throw new DomainException(AppErrorCode.MODULE_SKILLS_NOT_FOUND);
        }

        try {
            int copied = 0;
            int skipped = 0;
            for(CatalogEntry skill : skills) {
                Path dir = base.resolve(skill.location());
                boolean brandNew = !Files.exists(dir);
                String label = type + ": " + relative(project, dir);

                if(dryRun) {
                    (brandNew ? result.created() : result.skipped()).add(brandNew ? label : label + " (ya existe)");
                    continue;
                }

                ClasspathResources.CopyStats stats = ClasspathResources.copyTreeIfAbsent(skill.resource(), dir)
                    .orElseThrow(() -> new DomainException(AppErrorCode.MODULE_SKILLS_NOT_FOUND));
                copied += stats.copied();
                skipped += stats.skipped();
                if(type == ProviderType.CODEX)
                    addCodexHelper(skill, base, dir, brandNew);
                if(brandNew)
                    created.add(new Created(skill, base, ""));
            }

            if(copied > 0)
                result.created().add(type + ": skills (" + copied + " ficheros copiados)");
            if(skipped > 0)
                result.skipped().add(type + ": skills (" + skipped + " ficheros ya existían)");
        } catch(IOException e) {
            fail(result, type + ": no se pudieron copiar las skills", e);
        }
    }

    // Cada skill de Codex lleva agents/openai.yaml. Si la skill ya existía (p. ej. la instaló Gemini en la misma
    // carpeta .agents/skills) y estaba sin tocar, su registro se actualiza para que el fichero añadido no cuente como cambio
    private void addCodexHelper(CatalogEntry skill, Path base, Path dir, boolean brandNew) throws IOException {
        Path helper = dir.resolve(CODEX_HELPER_FILE);
        if(Files.exists(helper))
            return;

        Path key = dir.toAbsolutePath().normalize();
        Optional<ArtifactRecord> record = brandNew ? Optional.empty() : metadata.artifact(key);
        boolean untouched = record.isPresent() && record.get().hash().equals(inventory.inspect(skill, base).hash());

        writeCodexHelper(dir);
        if(untouched)
            metadata.putArtifactHash(key, record.get().groupId(), inventory.inspect(skill, base).hash(), record.get().state());
    }

    private static void writeCodexHelper(Path dir) throws IOException {
        Path helper = dir.resolve(CODEX_HELPER_FILE);
        String yaml = ClasspathResources.readText(HELPER).orElseThrow(() -> new IOException("Resource '" + HELPER + "' not found"));
        Files.createDirectories(helper.getParent());
        Files.writeString(helper, yaml);
    }

    // Agente: cabecera del proveedor + cuerpo (el del grupo si existe, si no la plantilla) con los placeholders resueltos.
    // Perfil (Codex): el recurso con el módulo resuelto.
    private void generateEntry(Target target, TextStore text, OperationResult result, List<Created> created) {
        CatalogEntry entry = target.entry();
        ProviderType type = entry.provider();
        Path path = target.base().resolve(entry.location());
        String label = type + ": " + relative(target.project(), path);

        if(Files.exists(path)) {
            result.skipped().add(label + " (ya existe)");
            return;
        }

        try {
            Optional<Composed> composed = compose(entry, label, result);
            if(composed.isEmpty())
                return;

            if(target.dryRun()) {
                result.created().add(label);
                return;
            }
            text.write(path, composed.get().content());
            // El router de Codex es una skill: lleva su agents/openai.yaml como las demás
            if(type == ProviderType.CODEX && ProviderLayout.isRouter(entry))
                writeCodexHelper(path.getParent());
            result.created().add(label);
            created.add(new Created(entry, target.base(), composed.get().shared()));
        } catch(IOException | DomainException e) {
            fail(result, label + ": no se pudo escribir", e);
        }
    }

    private record Composed(String content, String shared) {}

    // Vacío (con aviso) si falta el recurso o su cabecera no es válida
    private Optional<Composed> compose(CatalogEntry entry, String label, OperationResult result) throws IOException {
        ProviderType type = entry.provider();
        Optional<String> header = ClasspathResources.readText(entry.resource());
        Optional<String> template = entry.kind() == ArtifactKind.AGENT
            ? ClasspathResources.readText(entry.bodyResource()) : Optional.of("");
        if(header.isEmpty() || template.isEmpty()) {
            warn(result, label + ": no existe el recurso " + (header.isEmpty() ? entry.resource() : entry.bodyResource()));
            return Optional.empty();
        }

        if(entry.kind() == ArtifactKind.PROFILE)
            return Optional.of(new Composed(BodyPlaceholders.resolve(header.get(), type, entry.module()), ""));

        String renderedHeader;
        try {
            renderedHeader = HeaderMarkers.render(header.get(), os, type);
        } catch(DomainException e) {
            warn(result, label + ": marcador de cabecera no válido en " + entry.resource());
            return Optional.empty();
        }

        String shared = sharedBodyFor(entry).orElse(template.get().strip() + "\n");
        // La cabecera también lleva placeholders (nombre del agente); el cuerpo compartido se guarda sin resolver.
        // El router no tiene módulo: sus {module} se dejan intactos
        String content = entry.format().assemble(BodyPlaceholders.resolve(renderedHeader, type, entry.module()),
            BodyPlaceholders.resolve(shared, type, entry.module()));
        return Optional.of(new Composed(content, shared));
    }

    // El cuerpo compartido del grupo, si ya existe y no está vacío
    private Optional<String> sharedBodyFor(CatalogEntry entry) {
        return metadata.group(GroupIds.of(entry))
            .map(GroupRecord::sharedBody)
            .filter(body -> !body.isBlank());
    }

    private static String relative(Path project, Path path) {
        return project.relativize(path).toString().replace('\\', '/');
    }

    // Registra grupos y artefactos nuevos con el hash de lo que quedó en disco
    private void register(List<Created> created, OperationResult result) {
        for(Created c : created) {
            CatalogEntry entry = c.entry();
            try {
                String groupId = GroupIds.of(entry);
                if(metadata.group(groupId).isEmpty())
                    metadata.putGroup(new GroupRecord(groupId, ArtifactScope.PROJECT, c.sharedBody()));

                Path root = c.base().toAbsolutePath().normalize();
                String hash = inventory.inspect(entry, root).hash();
                metadata.putArtifactHash(root.resolve(entry.location()), groupId, hash, LinkState.LINKED);
            } catch(DomainException e) {
                fail(result, entry.provider() + ": " + entry.relativePath() + " creado pero no se pudo registrar", e);
            }
        }
    }

    private void fail(OperationResult result, String message, Exception cause) {
        log.error(message, cause);
        result.warnings().add(message);
    }

    private void warn(OperationResult result, String message) {
        log.warn(message);
        result.warnings().add(message);
    }
}
