package es.vargontoc.agentic.develop.cli.application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.metadata.ContentHash;
import es.vargontoc.agentic.develop.cli.domain.team.ModuleEntry;
import es.vargontoc.agentic.develop.cli.domain.team.ModuleName;
import es.vargontoc.agentic.develop.cli.domain.team.TeamOptions;
import es.vargontoc.agentic.develop.cli.domain.team.TeamParser;
import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning;
import es.vargontoc.agentic.develop.cli.domain.diff.TextDiff;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

/**
 * Lectura y escritura estructurada de {@code .sdd/team.md}. Solo reescribe las
 * líneas de las secciones {@code ## Módulos} y {@code ## Opciones}; conserva el
 * resto del fichero y delega en {@link TextStore} la copia y el fin de línea.
 */
public class TeamService {

    public record ModuleSpec(String name, String mapId, List<String> paths) {
        public ModuleSpec {
            paths = paths == null ? List.of() : List.copyOf(paths);
        }
    }

    public record TeamView(Path path, String hash, List<ModuleSpec> modules, TeamOptions options,
                           List<TeamWarning> warnings) {}

    public record UpdateResult(Path path, boolean dryRun, boolean written, Path backup, String hash,
                               String diff, List<TeamWarning> warnings) {}

    private final PathResolver paths;
    private final Path backupRoot;

    public TeamService(PathResolver paths, Path backupRoot) {
        this.paths = paths;
        this.backupRoot = backupRoot;
    }

    public TeamView read(Path project) {
        Path team = teamPath(project);
        if(!Files.isRegularFile(team))
            throw new DomainException(AppErrorCode.TEAM_FILE_NOT_FOUND);

        TextStore.TextFile file = store(project).read(team);
        TeamParser.TeamDocument document = TeamParser.parseDocument(file.content());
        return new TeamView(team, ContentHash.of(file.content()), toSpecs(document.modules()),
            document.options(), document.warnings());
    }

    public UpdateResult update(Path project, TeamOptions requestedOptions, List<ModuleSpec> requestedModules,
                               String expectHash, boolean dryRun) {
        Path team = teamPath(project);
        if(!Files.isRegularFile(team))
            throw new DomainException(AppErrorCode.TEAM_FILE_NOT_FOUND);

        TextStore text = store(project);
        String current = text.read(team).content();
        String currentHash = ContentHash.of(current);
        if(expectHash != null && !expectHash.isBlank() && !expectHash.equals(currentHash))
            throw new DomainException(AppErrorCode.TEAM_CONFLICT);

        List<ModuleSpec> modules = validateModules(requestedModules);
        TeamOptions options = validateOptions(requestedOptions, modules);

        String composed = replaceSection(current, TeamParser.MODULES_HEADER, moduleLines(modules));
        composed = replaceSection(composed, TeamParser.OPTIONS_HEADER, optionLines(options));

        boolean changed = !composed.equals(current);
        String diff = changed ? TextDiff.between(current, composed) : "";
        if(dryRun) {
            return new UpdateResult(team, true, changed, null, ContentHash.of(composed), diff,
                TeamParser.parseDocument(composed).warnings());
        }

        TextStore.WriteResult written = text.write(team, composed);
        String resultContent = written.written() ? composed : current;
        return new UpdateResult(team, false, written.written(), written.backup(),
            ContentHash.of(resultContent), diff, TeamParser.parseDocument(resultContent).warnings());
    }

    private Path teamPath(Path project) {
        if(!Files.exists(project))
            throw new DomainException(AppErrorCode.PROJECT_PATH_NOT_FOUND);
        if(!Files.isDirectory(project))
            throw new DomainException(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY);
        return project.resolve(ProjectWorkspace.SDD_DIR).resolve(ProjectWorkspace.TEAM_FILE);
    }

    private TextStore store(Path project) {
        return new TextStore(paths, List.of(project), backupRoot);
    }

    private static List<ModuleSpec> toSpecs(List<ModuleEntry> modules) {
        return modules.stream()
            .map(m -> new ModuleSpec(m.name(), m.mapId(), m.paths()))
            .toList();
    }

    private static List<ModuleSpec> validateModules(List<ModuleSpec> requested) {
        List<ModuleSpec> modules = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for(ModuleSpec raw : requested == null ? List.<ModuleSpec>of() : requested) {
            String name = ModuleName.normalize(raw.name());
            if(!names.add(name))
                throw new DomainException(AppErrorCode.TEAM_MODULE_DUPLICATE);

            String mapId = raw.mapId() == null ? "" : raw.mapId().trim();
            if(mapId.isEmpty() || "-".equals(mapId))
                mapId = null;
            else if(mapId.matches(".*\\s.*") || mapId.contains("|"))
                throw new DomainException(AppErrorCode.TEAM_MODULE_INVALID);

            List<String> modulePaths = new ArrayList<>();
            for(String path : raw.paths() == null ? List.<String>of() : raw.paths()) {
                String value = path == null ? "" : path.trim();
                if(value.isEmpty())
                    continue;
                if(value.matches(".*\\s.*") || value.contains("|"))
                    throw new DomainException(AppErrorCode.TEAM_MODULE_INVALID);
                modulePaths.add(value);
            }
            modules.add(new ModuleSpec(name, mapId, modulePaths));
        }
        return modules;
    }

    private static TeamOptions validateOptions(TeamOptions requested, List<ModuleSpec> modules) {
        TeamOptions options = (requested == null ? TeamOptions.DEFAULTS : requested).withDefaults();
        if(!TeamOptions.validSprints(options.sprints())
            || !TeamOptions.validGitPolicy(options.gitPolicy())
            || !TeamOptions.validContractStrategy(options.contractStrategy()))
            throw new DomainException(AppErrorCode.TEAM_OPTION_INVALID);

        String readmeOwner = options.readmeOwner();
        if(readmeOwner != null) {
            readmeOwner = readmeOwner.trim();
            if(readmeOwner.isEmpty() || "-".equals(readmeOwner))
                readmeOwner = null;
            else if(modules.stream().noneMatch(m -> m.name().equals(options.readmeOwner().trim())))
                throw new DomainException(AppErrorCode.TEAM_OPTION_INVALID);
        }
        return new TeamOptions(options.sprints(), options.gitPolicy(), options.contractStrategy(), readmeOwner);
    }

    private static List<String> moduleLines(List<ModuleSpec> modules) {
        return modules.stream()
            .map(m -> "- " + m.name() + " | " + (m.mapId() == null ? "-" : m.mapId()) + " | "
                + (m.paths().isEmpty() ? "-" : String.join(" ", m.paths())))
            .toList();
    }

    private static List<String> optionLines(TeamOptions options) {
        List<String> lines = new ArrayList<>(List.of(
            "- sprints: " + options.sprints(),
            "- git-policy: " + options.gitPolicy(),
            "- contract-strategy: " + options.contractStrategy()));
        if(options.readmeOwner() != null)
            lines.add("- readme-owner: " + options.readmeOwner());
        return lines;
    }

    /**
     * Sustituye las líneas de lista de una sección por {@code items}. Los comentarios
     * y líneas no-lista se conservan; si la sección no existe se añade al final.
     */
    private static String replaceSection(String content, String headerText, List<String> items) {
        List<String> lines = new ArrayList<>(List.of(content.split("\\R", -1)));
        int header = indexOfHeader(lines, headerText);

        if(header < 0) {
            if(!lines.isEmpty() && !lines.getLast().isBlank())
                lines.add("");
            lines.add(headerText);
            lines.add("");
            lines.addAll(items);
            return String.join("\n", lines);
        }

        int end = header + 1;
        while(end < lines.size() && !lines.get(end).trim().startsWith("## "))
            end++;

        int firstItem = -1;
        for(int i = header + 1; i < end; i++) {
            if(lines.get(i).trim().startsWith("-")) {
                firstItem = i;
                break;
            }
        }

        int at;
        if(firstItem >= 0) {
            // Se sustituyen solo las líneas de lista; comentarios y líneas no-lista se conservan.
            List<String> body = new ArrayList<>();
            boolean inserted = false;
            for(int i = header + 1; i < end; i++) {
                if(lines.get(i).trim().startsWith("-")) {
                    if(!inserted) {
                        body.addAll(items);
                        inserted = true;
                    }
                } else {
                    body.add(lines.get(i));
                }
            }
            if(!inserted)
                body.addAll(items);
            lines.subList(header + 1, end).clear();
            lines.addAll(header + 1, body);
            at = header + 1 + body.size();
        } else {
            at = end;
            lines.addAll(at, items);
            at = at + items.size();
        }

        // Una sección nueva no debe quedar pegada al siguiente encabezado.
        if(at < lines.size() && !lines.get(at).isBlank() && lines.get(at).trim().startsWith("## "))
            lines.add(at, "");
        return String.join("\n", lines);
    }

    private static int indexOfHeader(List<String> lines, String header) {
        for(int i = 0; i < lines.size(); i++) {
            if(header.equals(lines.get(i).trim()))
                return i;
        }
        return -1;
    }
}
