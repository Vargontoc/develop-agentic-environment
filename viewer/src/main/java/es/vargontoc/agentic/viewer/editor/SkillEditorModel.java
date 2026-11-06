package es.vargontoc.agentic.viewer.editor;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.vargontoc.agentic.viewer.cli.model.DescribeData;

/**
 * Modelo de edición para una skill con múltiples ficheros.
 * Mantiene buffers separados para cada fichero y detecta cambios.
 */
public final class SkillEditorModel {

    private final Path skillPath;
    private final Path project;
    private final List<String> files;
    private final Map<String, String> originalContents;
    private final Map<String, String> currentContents;
    private String selectedFile;

    public SkillEditorModel(DescribeData data, Path project, Map<String, String> initialContents) {
        this.skillPath = data.path() == null ? null : Path.of(data.path());
        this.project = project;
        this.files = data.files() == null ? List.of() : List.copyOf(data.files());
        this.originalContents = new HashMap<>(initialContents);
        this.currentContents = new HashMap<>(initialContents);
        this.selectedFile = files.isEmpty() ? null : files.getFirst();
    }

    public Path skillPath() {
        return skillPath;
    }

    public Path project() {
        return project;
    }

    public List<String> files() {
        return files;
    }

    public String selectedFile() {
        return selectedFile;
    }

    public void selectFile(String file) {
        if (files.contains(file)) {
            this.selectedFile = file;
        }
    }

    public String getContent(String file) {
        return currentContents.getOrDefault(file, "");
    }

    public void setContent(String file, String content) {
        if (files.contains(file)) {
            currentContents.put(file, content);
        }
    }

    public boolean isDirty(String file) {
        String original = originalContents.getOrDefault(file, "");
        String current = currentContents.getOrDefault(file, "");
        return !original.equals(current);
    }

    public boolean isDirty() {
        return files.stream().anyMatch(this::isDirty);
    }

    public void markSaved(String file) {
        String current = currentContents.get(file);
        if (current != null) {
            originalContents.put(file, current);
        }
    }

    public void markAllSaved() {
        for (String file : files) {
            markSaved(file);
        }
    }

    /**
     * Devuelve el contenido original de un fichero (para comparar).
     */
    public String getOriginalContent(String file) {
        return originalContents.getOrDefault(file, "");
    }
}
