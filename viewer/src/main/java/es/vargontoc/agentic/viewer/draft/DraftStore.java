package es.vargontoc.agentic.viewer.draft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.agentic.viewer.editor.EditorModel;
import es.vargontoc.agentic.viewer.editor.SkillEditorModel;
import es.vargontoc.agentic.viewer.runtime.AppPaths;
import es.vargontoc.agentic.viewer.runtime.ViewerLog;

/**
 * Guarda copias de trabajo de los editores sucios para poder recuperarlas si
 * la aplicación se cierra de forma inesperada. Es un fichero JSON pequeño en la
 * carpeta de estado del visor.
 */
public final class DraftStore {

    private static final Duration MAX_AGE = Duration.ofDays(30);

    public record Draft(
        String path,
        String project,
        String kind,
        String header,
        String body,
        String sharedBody,
        Map<String, String> files,
        long savedAt
    ) {
        public Draft {
            files = files == null ? Map.of() : Map.copyOf(files);
        }
    }

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public DraftStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    public static DraftStore defaultStore() {
        return new DraftStore(AppPaths.draftsFile());
    }

    public Path file() {
        return file;
    }

    public synchronized List<Draft> load() {
        return loadUnlocked();
    }

    public synchronized Optional<Draft> find(String artifactPath) {
        if (artifactPath == null || artifactPath.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(artifactPath);
        return loadUnlocked().stream()
            .filter(draft -> normalized.equals(draft.path()))
            .findFirst();
    }

    /** Guarda una copia del editor si tiene cambios. */
    public synchronized void save(EditorModel editor) {
        if (editor == null || editor.path() == null || !editor.isDirty()) {
            return;
        }
        put(new Draft(
            normalize(editor.path().toString()),
            editor.project() == null ? null : editor.project().toAbsolutePath().normalize().toString(),
            editor.kind(),
            editor.header(),
            editor.body(),
            editor.sharedBody(),
            Map.of(),
            Instant.now().toEpochMilli()));
    }

    /** Guarda una copia de todos los ficheros de una skill si tiene cambios. */
    public synchronized void save(SkillEditorModel editor) {
        if (editor == null || editor.skillPath() == null || !editor.isDirty()) {
            return;
        }
        Map<String, String> files = new LinkedHashMap<>();
        for (String name : editor.files()) {
            files.put(name, editor.getContent(name));
        }
        put(new Draft(
            normalize(editor.skillPath().toString()),
            editor.project() == null ? null : editor.project().toAbsolutePath().normalize().toString(),
            "skill",
            null,
            null,
            null,
            files,
            Instant.now().toEpochMilli()));
    }

    /** Inserta o reemplaza un borrador ya construido. */
    public synchronized void put(Draft draft) {
        if (draft == null || draft.path() == null) {
            return;
        }
        List<Draft> drafts = new ArrayList<>(loadUnlocked());
        drafts.removeIf(existing -> draft.path().equals(existing.path()));
        drafts.add(draft);
        drafts.removeIf(this::isExpired);
        writeUnlocked(drafts);
    }

    public synchronized void delete(String artifactPath) {
        if (artifactPath == null || artifactPath.isBlank()) {
            return;
        }
        String normalized = normalize(artifactPath);
        List<Draft> drafts = new ArrayList<>(loadUnlocked());
        boolean changed = drafts.removeIf(draft -> normalized.equals(draft.path()));
        if (changed) {
            writeUnlocked(drafts);
        }
    }

    public synchronized void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            ViewerLog.warn("No se pudieron borrar los borradores: " + exception.getMessage());
        }
    }

    public synchronized boolean hasDrafts() {
        return !loadUnlocked().isEmpty();
    }

    private List<Draft> loadUnlocked() {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            List<Draft> drafts = mapper.readValue(file.toFile(), new TypeReference<List<Draft>>() { });
            drafts.removeIf(this::isExpired);
            return drafts;
        } catch (IOException | RuntimeException exception) {
            ViewerLog.warn("No se pudieron leer los borradores: " + exception.getMessage());
            return List.of();
        }
    }

    private void writeUnlocked(List<Draft> drafts) {
        try {
            Files.createDirectories(file.getParent());
            if (drafts.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), drafts);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException exception) {
            ViewerLog.warn("No se pudieron guardar los borradores: " + exception.getMessage());
        }
    }

    private boolean isExpired(Draft draft) {
        long savedAt = draft.savedAt();
        return savedAt <= 0 || Instant.ofEpochMilli(savedAt).plus(MAX_AGE).isBefore(Instant.now());
    }

    private static String normalize(String path) {
        return Path.of(path).toAbsolutePath().normalize().toString();
    }
}
