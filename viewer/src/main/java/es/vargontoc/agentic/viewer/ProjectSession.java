package es.vargontoc.agentic.viewer;

import java.nio.file.Path;
import java.util.Objects;

import es.vargontoc.agentic.viewer.cli.CliClient;
import es.vargontoc.agentic.viewer.cli.model.InitData;
import es.vargontoc.agentic.viewer.cli.model.StatusData;

/**
 * Sesión de un único proyecto: guarda la ruta, su estado y permite iniciarlo.
 * Toda la validación y la escritura las hace el CLI.
 */
public final class ProjectSession {

    private final CliClient client;
    private Path path;
    private StatusData status;

    public ProjectSession(CliClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public StatusData select(Path project) {
        Objects.requireNonNull(project, "project");
        this.path = project.toAbsolutePath().normalize();
        this.status = client.status(this.path);
        return status;
    }

    public InitData init() {
        if (path == null) {
            throw new IllegalStateException("No hay proyecto seleccionado.");
        }
        InitData result = client.init(path);
        this.status = client.status(path);
        return result;
    }

    public void clear() {
        path = null;
        status = null;
    }

    public Path path() {
        return path;
    }

    public StatusData status() {
        return status;
    }

    public boolean configured() {
        return status != null
            && status.project() != null
            && status.project().initialized();
    }
}
