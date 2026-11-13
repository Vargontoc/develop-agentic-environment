package es.vargontoc.agentic.viewer.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lista de proyectos recientes, ordenada del más reciente al más antiguo.
 * Es un JSON pequeño en la carpeta de estado del visor; los proyectos que ya
 * no existen se ocultan de la lista pero se conservan hasta que se limpien.
 */
public final class RecentProjectsStore {

    public static final int MAX = 8;

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public RecentProjectsStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    public static RecentProjectsStore defaultStore() {
        return new RecentProjectsStore(AppPaths.recentProjectsFile());
    }

    public Path file() {
        return file;
    }

    /** Rutas existentes, de la más reciente a la más antigua. */
    public synchronized List<Path> list() {
        List<Path> result = new ArrayList<>();
        for (String raw : loadUnlocked()) {
            Path path = toPath(raw);
            if (path != null && Files.isDirectory(path)) {
                result.add(path);
            }
        }
        return result;
    }

    /** Proyecto a reabrir al arrancar: el más reciente que siga existiendo. */
    public synchronized Optional<Path> lastExisting() {
        return list().stream().findFirst();
    }

    /** Mueve el proyecto al principio de la lista, sin duplicados y con tope. */
    public synchronized void remember(Path project) {
        if (project == null) {
            return;
        }
        Path normalized = project.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            return;
        }
        List<String> projects = new ArrayList<>();
        projects.add(normalized.toString());
        for (String raw : loadUnlocked()) {
            Path existing = toPath(raw);
            if (existing == null || existing.equals(normalized) || !Files.isDirectory(existing)) {
                continue;
            }
            projects.add(existing.toString());
            if (projects.size() >= MAX) {
                break;
            }
        }
        writeUnlocked(projects);
    }

    public synchronized void remove(Path project) {
        if (project == null) {
            return;
        }
        String normalized = project.toAbsolutePath().normalize().toString();
        List<String> projects = new ArrayList<>(loadUnlocked());
        boolean changed = projects.removeIf(raw -> normalized.equals(normalizeRaw(raw)));
        if (changed) {
            writeUnlocked(projects);
        }
    }

    public synchronized void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            ViewerLog.warn("No se pudo limpiar la lista de recientes: " + exception.getMessage());
        }
    }

    private List<String> loadUnlocked() {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            List<String> projects = mapper.readValue(file.toFile(), new TypeReference<List<String>>() { });
            return projects == null ? List.of() : projects.stream().filter(p -> p != null && !p.isBlank()).toList();
        } catch (IOException | RuntimeException exception) {
            ViewerLog.warn("No se pudieron leer los proyectos recientes: " + exception.getMessage());
            return List.of();
        }
    }

    private void writeUnlocked(List<String> projects) {
        try {
            Files.createDirectories(file.getParent());
            if (projects.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), projects);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException exception) {
            ViewerLog.warn("No se pudieron guardar los proyectos recientes: " + exception.getMessage());
        }
    }

    private static Path toPath(String raw) {
        try {
            return Path.of(raw).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String normalizeRaw(String raw) {
        Path path = toPath(raw);
        return path == null ? raw : path.toString();
    }
}
