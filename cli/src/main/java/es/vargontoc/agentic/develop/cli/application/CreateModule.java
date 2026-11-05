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
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;
import es.vargontoc.agentic.develop.cli.domain.format.BodyPlaceholders;
import es.vargontoc.agentic.develop.cli.domain.format.HeaderMarkers;
import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
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
    private static final String AGENTS_RESOURCE = "/project/agents/";
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
            if(!type.supportsProject()) {
                warn(result, type + ": aún no se soporta a nivel de proyecto, se omite");
                continue;
            }

            Path providerDir = project.resolve(type.projectDir());
            try {
                paths.requireWithin(project, providerDir);
            } catch(DomainException e) {
                warn(result, type + ": " + type.projectDir() + " queda fuera del proyecto, se omite");
                continue;
            }

            if(withSkills)
                copySkills(providerDir, type, dryRun, result, created);

            for(Technician technician : technicians) {
                String slug = technician.slug();
                // El router es único por técnico y proveedor; el especialista depende del módulo
                generateAgent(new AgentRequest(providerDir, type, module, slug + "-router", slug + "-router-template",
                    slug + "-router", dryRun), text, result, created);
                generateAgent(new AgentRequest(providerDir, type, module, slug, slug + "-template", slug + "-" + module, dryRun),
                    text, result, created);
            }
        }

        register(created, module, result);
    }

    // Artefacto nuevo pendiente de registrar: su carpeta de proveedor y su ruta relativa (fichero o skill)
    private record Created(ProviderType type, Path providerDir, String relativePath, String sharedBody) {}

    private record AgentRequest(Path providerDir, ProviderType type, String module, String headerName,
                                String templateName, String targetName, boolean dryRun) {}

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
    private void copySkills(Path providerDir, ProviderType type, boolean dryRun, OperationResult result, List<Created> created) {
        try {
            Optional<List<String>> skills = ClasspathResources.listDirectories(SKILLS_RESOURCE);
            if(skills.isEmpty()) {
                log.error("Resource '{}' not found", SKILLS_RESOURCE);
                throw new DomainException(AppErrorCode.MODULE_SKILLS_NOT_FOUND);
            }

            List<String> brandNew = skills.get().stream()
                .filter(name -> !Files.exists(providerDir.resolve("skills").resolve(name)))
                .toList();

            if(dryRun) {
                brandNew.forEach(name -> result.created().add(type + ": skills/" + name));
                skills.get().stream().filter(name -> !brandNew.contains(name))
                    .forEach(name -> result.skipped().add(type + ": skills/" + name + " (ya existe)"));
                return;
            }

            Optional<ClasspathResources.CopyStats> stats =
                ClasspathResources.copyTreeIfAbsent(SKILLS_RESOURCE, providerDir.resolve("skills"));
            if(stats.isEmpty())
                throw new DomainException(AppErrorCode.MODULE_SKILLS_NOT_FOUND);

            if(stats.get().copied() > 0)
                result.created().add(type + ": skills (" + stats.get().copied() + " ficheros copiados)");
            if(stats.get().skipped() > 0)
                result.skipped().add(type + ": skills (" + stats.get().skipped() + " ficheros ya existían)");
            brandNew.forEach(name -> created.add(new Created(type, providerDir, "skills/" + name, "")));
        } catch(IOException e) {
            fail(result, type + ": no se pudieron copiar las skills", e);
        }
    }

    // Une cabecera del proveedor + cuerpo (el del grupo si existe, si no la plantilla) y resuelve los placeholders
    private void generateAgent(AgentRequest request, TextStore text, OperationResult result, List<Created> created) {
        ProviderType type = request.type();
        String relative = "agents/" + request.targetName() + ".md";
        Path target = request.providerDir().resolve(relative);
        String label = type + ": " + type.projectDir() + "/" + relative;

        if(Files.exists(target)) {
            result.skipped().add(label + " (ya existe)");
            return;
        }

        String headerResource = AGENTS_RESOURCE + type.resourceDir() + "/" + request.headerName() + ".md";
        String templateResource = AGENTS_RESOURCE + "shared/" + request.templateName() + ".md";
        try {
            Optional<String> header = ClasspathResources.readText(headerResource);
            Optional<String> template = ClasspathResources.readText(templateResource);
            if(header.isEmpty() || template.isEmpty()) {
                warn(result, label + ": no existe el recurso " + (header.isEmpty() ? headerResource : templateResource));
                return;
            }

            String renderedHeader;
            try {
                renderedHeader = HeaderMarkers.render(header.get(), os, type);
            } catch(DomainException e) {
                warn(result, label + ": marcador de cabecera no válido en " + headerResource);
                return;
            }

            String shared = sharedBodyFor(type, request.module(), request.targetName())
                .orElse(template.get().strip() + "\n");
            // La cabecera también lleva placeholders (nombre del agente); el cuerpo compartido se guarda sin resolver
            String module = isRouter(request) ? null : request.module();
            String content = ArtifactFormat.MARKDOWN.assemble(BodyPlaceholders.resolve(renderedHeader, type, module),
                BodyPlaceholders.resolve(shared, type, module));

            if(request.dryRun()) {
                result.created().add(label);
                return;
            }
            text.write(target, content);
            result.created().add(label);
            created.add(new Created(type, request.providerDir(), relative, shared));
        } catch(IOException | DomainException e) {
            fail(result, label + ": no se pudo escribir", e);
        }
    }

    private static boolean isRouter(AgentRequest request) {
        return request.targetName().endsWith("-router");
    }

    // El cuerpo compartido del grupo, si ya existe y no está vacío
    private Optional<String> sharedBodyFor(ProviderType type, String module, String targetName) {
        return catalog.entries(type, ArtifactScope.PROJECT, List.of(module)).stream()
            .filter(e -> e.relativePath().equals("agents/" + targetName + ".md"))
            .findFirst()
            .map(GroupIds::of)
            .flatMap(metadata::group)
            .map(GroupRecord::sharedBody)
            .filter(body -> !body.isBlank());
    }

    // Registra grupos y artefactos nuevos con el hash de lo que quedó en disco
    private void register(List<Created> created, String module, OperationResult result) {
        for(Created c : created) {
            try {
                Optional<CatalogEntry> entry = catalog.entries(c.type(), ArtifactScope.PROJECT, List.of(module)).stream()
                    .filter(e -> e.relativePath().equals(c.relativePath()))
                    .findFirst();
                if(entry.isEmpty()) {
                    warn(result, c.type() + ": " + c.relativePath() + " no figura en el catálogo y no se registra");
                    continue;
                }

                String groupId = GroupIds.of(entry.get());
                if(metadata.group(groupId).isEmpty())
                    metadata.putGroup(new GroupRecord(groupId, ArtifactScope.PROJECT, c.sharedBody()));

                Path root = c.providerDir().toAbsolutePath().normalize();
                String hash = inventory.inspect(entry.get(), root).hash();
                metadata.putArtifactHash(root.resolve(c.relativePath()), groupId, hash, LinkState.LINKED);
            } catch(DomainException e) {
                fail(result, c.type() + ": " + c.relativePath() + " creado pero no se pudo registrar", e);
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
