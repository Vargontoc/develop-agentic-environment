package es.vargontoc.agentic.develop.cli.domain.team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning.Type;

/**
 * Lee la sección "## Módulos" de {@code team.md} de forma tolerante: las líneas mal formadas y los módulos repetidos
 * se anotan como avisos y el resto se devuelve.
 */
public final class TeamParser {

    public static final String MODULES_HEADER = "## Módulos";

    private static final String NONE = "-";
    private static final int FIELDS = 3;

    private TeamParser() {}

    public static TeamInfo parse(String content) {
        String[] lines = content.split("\\R", -1);
        List<ModuleEntry> modules = new ArrayList<>();
        List<TeamWarning> warnings = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        int header = indexOfHeader(lines);
        if(header < 0)
            return TeamInfo.withWarning(new TeamWarning(Type.SECTION_MISSING, 0));

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
        return new TeamInfo(modules, warnings);
    }

    // Un comentario HTML puede ocupar varias líneas
    private static boolean updateCommentState(String line, boolean inComment) {
        if(inComment)
            return !line.contains("-->");
        return line.startsWith("<!--") && !line.contains("-->");
    }

    private static int indexOfHeader(String[] lines) {
        for(int i = 0; i < lines.length; i++) {
            if(MODULES_HEADER.equals(lines[i].trim()))
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
