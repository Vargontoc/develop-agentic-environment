package es.vargontoc.agentic.viewer.editor;

import java.nio.file.Path;

import es.vargontoc.agentic.viewer.cli.model.DescribeData;

/**
 * Buffers de edición de un artefacto: cabecera, cuerpo y cuerpo compartido con
 * sus valores originales para detectar cambios. Puro y sin JavaFX, así se puede
 * probar en JUnit. La escritura real la hace el CLI.
 */
public final class EditorModel {

    private final Path path;
    private final Path project;
    private final String format;
    private final String kind;
    private final String module;

    private String originalHeader;
    private String originalBody;
    private String originalSharedBody;
    private String header;
    private String body;
    private String sharedBody;

    private EditorModel(Path path, Path project, String format, String kind, String module,
                        String header, String body, String sharedBody) {
        this.path = path;
        this.project = project;
        this.format = format;
        this.kind = kind;
        this.module = module;
        this.originalHeader = header == null ? "" : header;
        this.originalBody = body == null ? "" : body;
        this.originalSharedBody = sharedBody == null ? null : sharedBody;
        this.header = this.originalHeader;
        this.body = this.originalBody;
        this.sharedBody = this.originalSharedBody;
    }

    public static EditorModel of(DescribeData data, Path project) {
        return new EditorModel(
            data.path() == null ? null : Path.of(data.path()),
            project,
            data.format(),
            data.kind(),
            data.module(),
            data.header(),
            data.body(),
            data.sharedBody());
    }

    public Path path() {
        return path;
    }

    public Path project() {
        return project;
    }

    public String format() {
        return format;
    }

    public String kind() {
        return kind;
    }

    public String module() {
        return module;
    }

    public String sharedBody() {
        return sharedBody;
    }

    /** La cabecera solo es editable en agentes con formato (Markdown o TOML de Codex). */
    public boolean headerEditable() {
        return format != null;
    }

    /** El cuerpo es editable en agentes y perfiles; las skills se editan fichero a fichero (TASK-012). */
    public boolean bodyEditable() {
        return format != null || "profile".equals(kind);
    }

    /** Hay grupo al que propagar: el artefacto tiene un cuerpo compartido. */
    public boolean hasGroup() {
        return originalSharedBody != null;
    }

    public boolean hasPlaceholder() {
        return sharedBody != null && sharedBody.contains(SharedBody.MODULE_PLACEHOLDER);
    }

    public String header() {
        return header;
    }

    public String body() {
        return body;
    }

    public void setHeader(String value) {
        this.header = value == null ? "" : value;
    }

    public void setBody(String value) {
        this.body = value == null ? "" : value;
    }

    public void setSharedBody(String value) {
        this.sharedBody = value == null ? null : value;
    }

    public boolean headerDirty() {
        return headerEditable() && !header.equals(originalHeader);
    }

    public boolean bodyDirty() {
        return bodyEditable() && !body.equals(originalBody);
    }

    public boolean sharedBodyDirty() {
        return hasGroup() && !sharedBody.equals(originalSharedBody);
    }

    public boolean isDirty() {
        return headerDirty() || bodyDirty() || sharedBodyDirty();
    }

    public void markHeaderSaved() {
        originalHeader = header;
    }

    public void markBodySaved() {
        originalBody = body;
    }

    public void markSharedBodySaved() {
        originalSharedBody = sharedBody;
    }
}
