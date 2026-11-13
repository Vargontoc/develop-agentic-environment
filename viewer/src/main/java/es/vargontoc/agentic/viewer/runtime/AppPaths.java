package es.vargontoc.agentic.viewer.runtime;

import java.nio.file.Path;

/**
 * Rutas de estado del visor. Se mantienen separadas de los ficheros del
 * proyecto y del estado de la CLI.
 */
public final class AppPaths {

    private AppPaths() {}

    /** Carpeta de estado del visor: logs y borradores. */
    public static Path stateDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "agentic-develop", "viewer");
        }
        String xdgState = System.getenv("XDG_STATE_HOME");
        if (xdgState != null && !xdgState.isBlank()) {
            return Path.of(xdgState, "agentic-develop", "viewer");
        }
        return Path.of(System.getProperty("user.home"), ".local", "state", "agentic-develop", "viewer");
    }

    public static Path draftsFile() {
        return stateDirectory().resolve("drafts.json");
    }

    public static Path recentProjectsFile() {
        return stateDirectory().resolve("recent-projects.json");
    }

    public static Path logFile() {
        return stateDirectory().resolve("viewer.log");
    }
}
