package es.vargontoc.agentic.develop.cli.domain.team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning.Type;

/**
 * Lee las secciones "## Módulos" y "## Opciones" de {@code team.md} de forma tolerante: las líneas mal formadas,
 * las opciones desconocidas y los módulos repetidos se anotan como avisos y el resto se devuelve.
 */
public final class TeamParser {

    public static final String MODULES_HEADER = "## Módulos";
    public static final String OPTIONS_HEADER = "## Opciones";

    private static final String NONE = "-";
    private static final int FIELDS = 3;

    private TeamParser() {}

    /** Resultado completo de leer {@code team.md}. */
    public record TeamDocument(List<ModuleEntry> modules, TeamOptions options, List<TeamWarning> warnings) {
        public TeamDocument {
            modules = List.copyOf(modules);
            warnings = List.copyOf(warnings);
        }

        public TeamInfo info() {
            return new TeamInfo(modules, warnings);
        }

        public List<String> moduleNames() {
            return modules.stream().map(ModuleEntry::name).toList();
        }
    }

    public static TeamInfo parse(String content) {
        String[] lines = content.split("\\R", -1);
        List<TeamWarning> warnings = new ArrayList<>();
        return new TeamInfo(parseModules(lines, warnings), warnings);
    }

    public static TeamDocument parseDocument(String content) {
        String[] lines = content.split("\\R", -1);
        List<TeamWarning> warnings = new ArrayList<>();
        List<ModuleEntry> modules = parseModules(lines, warnings);
        TeamOptions options = parseOptions(lines, warnings,
            modules.stream().map(ModuleEntry::name).toList());
        return new TeamDocument(modules, options, warnings);
    }

    private static List<ModuleEntry> parseModules(String[] lines, List<TeamWarning> warnings) {
        List<ModuleEntry> modules = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        int header = indexOfHeader(lines, MODULES_HEADER);
        if(header < 0) {
            addWarning(warnings, Type.SECTION_MISSING, 0);
            return modules;
        }

        boolean inComment = false;
        for(int i = header + 1; i < lines.length && !lines[i].startsWith("## "); i++) {
            String line = lines[i].trim();
            inComment = updateCommentState(line, inComment);
            if(inComment || line.isEmpty() || line.startsWith("<!--") || line.endsWith("-->"))
                continue;

            ModuleEntry entry = parseLine(line, i + 1);
            if(entry == null)
                warnings.add(new TeamWarning(Type.MALFORMED_LINE, i + 1));
            else if(!seen.add(entry.name()))
                warnings.add(new TeamWarning(Type.DUPLICATE_MODULE, i + 1));
            else
                modules.add(entry);
        }
        return modules;
    }

    private static TeamOptions parseOptions(String[] lines, List<TeamWarning> warnings, List<String> moduleNames) {
        int header = indexOfHeader(lines, OPTIONS_HEADER);
        if(header < 0) {
            addWarning(warnings, Type.SECTION_MISSING, 0);
            return TeamOptions.DEFAULTS;
        }

        String sprints = TeamOptions.DEFAULTS.sprints();
        String gitPolicy = TeamOptions.DEFAULTS.gitPolicy();
        String contractStrategy = TeamOptions.DEFAULTS.contractStrategy();
        String readmeOwner = null;
        int readmeLine = 0;
        boolean inComment = false;

        for(int i = header + 1; i < lines.length && !lines[i].startsWith("## "); i++) {
            String line = lines[i].trim();
            inComment = updateCommentState(line, inComment);
            if(inComment || line.isEmpty() || !line.startsWith("-") || line.startsWith("<!--"))
                continue;

            int colon = line.indexOf(':');
            if(colon < 0) {
                warnings.add(new TeamWarning(Type.MALFORMED_LINE, i + 1));
                continue;
            }
            String key = line.substring(1, colon).trim().toLowerCase(java.util.Locale.ROOT);
            String value = line.substring(colon + 1).trim();

            switch(key) {
                case "sprints" -> {
                    if(TeamOptions.validSprints(value))
                        sprints = value;
                    else
                        warnings.add(new TeamWarning(Type.UNKNOWN_OPTION, i + 1));
                }
                case "git-policy" -> {
                    if(TeamOptions.validGitPolicy(value))
                        gitPolicy = value;
                    else
                        warnings.add(new TeamWarning(Type.UNKNOWN_OPTION, i + 1));
                }
                case "contract-strategy" -> {
                    if(TeamOptions.validContractStrategy(value))
                        contractStrategy = value;
                    else
                        warnings.add(new TeamWarning(Type.UNKNOWN_OPTION, i + 1));
                }
                case "readme-owner" -> {
                    readmeOwner = value.isBlank() || NONE.equals(value) ? null : value;
                    readmeLine = i + 1;
                }
                default -> warnings.add(new TeamWarning(Type.UNKNOWN_OPTION, i + 1));
            }
        }

        if(readmeOwner != null && !moduleNames.contains(readmeOwner))
            warnings.add(new TeamWarning(Type.UNKNOWN_OPTION, readmeLine));

        return new TeamOptions(sprints, gitPolicy, contractStrategy, readmeOwner);
    }

    private static void addWarning(List<TeamWarning> warnings, Type type, int line) {
        if(warnings.stream().noneMatch(w -> w.type() == type && w.line() == line))
            warnings.add(new TeamWarning(type, line));
    }

    // Un comentario HTML puede ocupar varias líneas
    private static boolean updateCommentState(String line, boolean inComment) {
        if(inComment)
            return !line.contains("-->");
        return line.startsWith("<!--") && !line.contains("-->");
    }

    private static int indexOfHeader(String[] lines, String header) {
        for(int i = 0; i < lines.length; i++) {
            if(header.equals(lines[i].trim()))
                return i;
        }
        return -1;
    }

    private static ModuleEntry parseLine(String line, int number) {
        if(!line.startsWith("-"))
            return null;

        String[] fields = line.substring(1).split("\\|", -1);
        if(fields.length != FIELDS)
            return null;

        String name = fields[0].trim();
        String id = fields[1].trim();
        String paths = fields[2].trim();
        if(name.isEmpty() || name.matches(".*\\s.*") || id.isEmpty() || paths.isEmpty())
            return null;

        return new ModuleEntry(name, NONE.equals(id) ? null : id,
            NONE.equals(paths) ? List.of() : List.of(paths.split("\\s+")), number);
    }
}
