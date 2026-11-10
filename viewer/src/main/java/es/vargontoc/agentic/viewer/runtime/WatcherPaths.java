package es.vargontoc.agentic.viewer.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Decide qué directorios vigila el visor y cuáles se ignoran. Se vigila el
 * proyecto y las carpetas globales de los proveedores, pero se saltan carpetas
 * pesadas que no contienen artefactos editables.
 */
public final class WatcherPaths {

    /** Nombres de directorio que nunca se recorren. */
    public static final Set<String> IGNORED_DIRECTORY_NAMES = Set.of(
        ".git", ".hg", ".svn", ".idea", ".gradle", ".m2", ".cache",
        "node_modules", "target", "build", "dist", "out", "bin", "obj",
        ".venv", "venv", "__pycache__", "vendor");

    private WatcherPaths() {}

    /**
     * Directorios a vigilar: solo las carpetas que pueden contener artefactos
     * del visor, tanto en el proyecto como en el ámbito global.
     */
    public static List<Path> directories(Path project, Path home) {
        List<Path> directories = new ArrayList<>();
        if (project != null) {
            addIfDirectory(directories, project.resolve(".claude"));
            addIfDirectory(directories, project.resolve(".opencode"));
            addIfDirectory(directories, project.resolve(".codex"));
            addIfDirectory(directories, project.resolve(".agents"));
            addIfDirectory(directories, project.resolve(".gemini"));
            addIfDirectory(directories, project.resolve(".sdd"));
        }
        if (home != null) {
            for (String relative : List.of(
                ".claude/agents", ".claude/skills",
                ".codex/agents", ".codex/profiles",
                ".agents/skills",
                ".gemini/skills", ".gemini/config/agents",
                ".config/opencode/agents", ".config/opencode/skills")) {
                addIfDirectory(directories, home.resolve(relative));
            }
        }
        return directories;
    }

    private static void addIfDirectory(List<Path> directories, Path candidate) {
        if (Files.isDirectory(candidate) && !directories.contains(candidate)) {
            directories.add(candidate);
        }
    }

    public static boolean isIgnored(Path directory) {
        if (directory == null || directory.getFileName() == null) {
            return false;
        }
        return IGNORED_DIRECTORY_NAMES.contains(directory.getFileName().toString());
    }
}
