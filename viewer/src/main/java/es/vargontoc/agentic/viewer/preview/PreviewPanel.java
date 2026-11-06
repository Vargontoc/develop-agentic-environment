package es.vargontoc.agentic.viewer.preview;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import es.vargontoc.agentic.viewer.editor.EditorModel;
import es.vargontoc.agentic.viewer.editor.SharedBody;
import es.vargontoc.agentic.viewer.editor.SkillEditorModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

/**
 * Panel central de previsualización y edición. Soporta:
 * - Agentes y perfiles: cabecera, cuerpo, cuerpo compartido
 * - Skills: múltiples ficheros editables
 * - Ficheros .sdd: solo lectura
 */
public final class PreviewPanel extends VBox {

    /** Acción de guardar un agente; recibe el editor con los buffers actuales. */
    public interface SaveListener {
        void onSave(EditorModel editor);
    }

    /** Acción de propagar el cuerpo compartido al grupo. */
    public interface PropagateListener {
        void onPropagate(EditorModel editor);
    }

    /** Acción de volver a vincular un artefacto legacy o desvinculado a su grupo. */
    public interface AdoptListener {
        void onAdopt(EditorModel editor);
    }

    /** Acción de guardar un fichero de skill. */
    public interface SaveSkillFileListener {
        void onSaveSkillFile(SkillEditorModel editor, String file);
    }

    /** Acción de propagar un fichero de skill al grupo. */
    public interface PropagateSkillFileListener {
        void onPropagateSkillFile(SkillEditorModel editor, String file);
    }

    private final MarkdownRenderer markdown = new MarkdownRenderer();

    private Consumer<Path> onSddFileSelected = file -> { };
    private SaveListener onSave = editor -> { };
    private PropagateListener onPropagate = editor -> { };
    private AdoptListener onAdopt = editor -> { };
    private SaveSkillFileListener onSaveSkillFile = (editor, file) -> { };
    private PropagateSkillFileListener onPropagateSkillFile = (editor, file) -> { };

    // Estado para agentes
    private EditorModel editor;
    private String status = "";
    private String headerLanguage = "";
    private String bodyLanguage = "";
    private boolean updatingFromModel = false;

    // Estado para skills
    private SkillEditorModel skillEditor;
    private boolean isSkillMode = false;

    private final Label title = new Label("Selecciona un artefacto");
    private final Label notice = new Label();
    private final VBox metaBox = new VBox(3);

    // Sección de cabecera (solo agentes)
    private final VBox headerSection = new VBox(6);
    private final Label headerTitle = new Label();
    private final TextArea headerEdit = new TextArea();

    // Sección de ficheros de skill
    private final VBox skillFilesSection = new VBox(6);
    private final ChoiceBox<String> skillFileChoice = new ChoiceBox<>();
    private final TextArea skillFileEdit = new TextArea();
    private final Button saveSkillFileButton = new Button("Guardar fichero");
    private final Button propagateSkillFileButton = new Button("Propagar fichero");

    // Sección de cuerpo compartido (solo agentes)
    private final VBox sharedSection = new VBox(6);
    private final Label sharedTitle = new Label();
    private final ToggleButton sharedResolved = new ToggleButton("Ver resuelto");
    private final TextArea sharedEdit = new TextArea();
    private final WebView sharedView = new WebView();
    private final Button propagateButton = new Button("Propagar");

    // Sección de cuerpo (solo agentes)
    private final VBox bodySection = new VBox(8);
    private final TextArea bodyEdit = new TextArea();
    private final WebView renderView = new WebView();
    private final ToggleButton textToggle = new ToggleButton("Texto");
    private final ToggleButton renderToggle = new ToggleButton("Render");

    // Acciones
    private final Button saveButton = new Button("Guardar");
    private final Button adoptButton = new Button("Volver al grupo");
    private final Label dirtyLabel = new Label();
    private final Label saveStatus = new Label();
    private final HBox actionBar = new HBox(10, saveButton, adoptButton, dirtyLabel, saveStatus);

    public PreviewPanel() {
        getStyleClass().add("preview-panel");
        setPadding(new Insets(14));
        setSpacing(10);

        title.getStyleClass().add("heading");
        title.setWrapText(true);

        notice.getStyleClass().add("readonly");
        notice.setWrapText(true);
        notice.setManaged(false);
        notice.setVisible(false);

        setupHeaderSection();
        setupSkillFilesSection();
        setupSharedSection();
        setupBodySection();
        setupActionButtons();

        getChildren().setAll(title, notice, metaBox, headerSection, skillFilesSection,
            sharedSection, bodySection, actionBar);

        showEmpty();
    }

    private void setupHeaderSection() {
        headerTitle.getStyleClass().add("section-title");
        headerEdit.setWrapText(false);
        headerEdit.getStyleClass().add("editor");
        headerEdit.setPrefRowCount(5);

        headerEdit.textProperty().addListener((observable, previous, current) -> {
            if (editor != null && !updatingFromModel) {
                editor.setHeader(current);
                updateDirty();
            }
        });
    }

    private void setupSkillFilesSection() {
        skillFileChoice.setPrefWidth(200);
        skillFileEdit.setWrapText(false);
        skillFileEdit.getStyleClass().add("editor");

        skillFileChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (skillEditor != null && newVal != null && !updatingFromModel) {
                // Guardar contenido actual antes de cambiar
                if (oldVal != null) {
                    skillEditor.setContent(oldVal, skillFileEdit.getText());
                }
                skillEditor.selectFile(newVal);
                updatingFromModel = true;
                skillFileEdit.setText(skillEditor.getContent(newVal));
                updatingFromModel = false;
            }
        });

        skillFileEdit.textProperty().addListener((observable, previous, current) -> {
            if (skillEditor != null && !updatingFromModel) {
                String selected = skillEditor.selectedFile();
                if (selected != null) {
                    skillEditor.setContent(selected, current);
                    updateDirty();
                }
            }
        });

        saveSkillFileButton.setOnAction(event -> {
            if (skillEditor != null) {
                String selected = skillEditor.selectedFile();
                if (selected != null && skillEditor.isDirty(selected)) {
                    onSaveSkillFile.onSaveSkillFile(skillEditor, selected);
                }
            }
        });

        propagateSkillFileButton.setOnAction(event -> {
            if (skillEditor != null) {
                String selected = skillEditor.selectedFile();
                if (selected != null) {
                    onPropagateSkillFile.onPropagateSkillFile(skillEditor, selected);
                }
            }
        });

        HBox skillActions = new HBox(8, saveSkillFileButton, propagateSkillFileButton);
        skillActions.setAlignment(Pos.CENTER_LEFT);

        skillFilesSection.getChildren().setAll(
            new Label("Fichero:"),
            skillFileChoice,
            skillFileEdit,
            skillActions
        );
    }

    private void setupSharedSection() {
        sharedTitle.getStyleClass().add("section-title");
        sharedEdit.setWrapText(false);
        sharedEdit.getStyleClass().add("editor");
        sharedEdit.setPrefRowCount(7);
        sharedView.setPrefHeight(140);
        sharedView.setMinHeight(100);
        sharedResolved.selectedProperty().addListener((observable, previous, selected) -> applySharedView());

        HBox sharedActions = new HBox(8, propagateButton, sharedResolved);
        sharedActions.setAlignment(Pos.CENTER_LEFT);
        StackPane sharedStack = new StackPane(sharedEdit, sharedView);
        sharedSection.getChildren().setAll(sharedTitle, sharedActions, sharedStack);

        sharedEdit.textProperty().addListener((observable, previous, current) -> {
            if (editor != null && !updatingFromModel) {
                editor.setSharedBody(current);
                updateDirty();
            }
        });
    }

    private void setupBodySection() {
        bodyEdit.setWrapText(false);
        bodyEdit.getStyleClass().add("editor");

        ToggleGroup group = new ToggleGroup();
        textToggle.setToggleGroup(group);
        renderToggle.setToggleGroup(group);
        textToggle.setSelected(true);
        group.selectedToggleProperty().addListener((observable, previous, selected) -> applyBodyView());

        HBox toggleRow = new HBox(8, textToggle, renderToggle);
        toggleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane bodyStack = new StackPane(bodyEdit, renderView);
        VBox.setVgrow(bodyStack, Priority.ALWAYS);
        bodySection.getChildren().setAll(toggleRow, bodyStack);
        VBox.setVgrow(bodySection, Priority.ALWAYS);

        bodyEdit.textProperty().addListener((observable, previous, current) -> {
            if (editor != null && !updatingFromModel) {
                editor.setBody(current);
                updateDirty();
            }
        });
    }

    private void setupActionButtons() {
        propagateButton.setOnAction(event -> {
            if (editor != null && editor.hasGroup()) {
                onPropagate.onPropagate(editor);
            }
        });

        adoptButton.setOnAction(event -> {
            if (editor != null) {
                onAdopt.onAdopt(editor);
            }
        });
        adoptButton.setManaged(false);
        adoptButton.setVisible(false);

        saveButton.setOnAction(event -> {
            if (editor != null && editor.isDirty()) {
                onSave.onSave(editor);
            }
        });
        saveButton.setDisable(true);

        dirtyLabel.getStyleClass().add("warning");
        dirtyLabel.setWrapText(true);
        saveStatus.setWrapText(true);

        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setPadding(new Insets(4, 0, 0, 0));
    }

    public void setOnSddFileSelected(Consumer<Path> handler) {
        this.onSddFileSelected = handler == null ? file -> { } : handler;
    }

    public void setOnSave(SaveListener listener) {
        this.onSave = listener == null ? editor -> { } : listener;
    }

    public void setOnPropagate(PropagateListener listener) {
        this.onPropagate = listener == null ? editor -> { } : listener;
    }

    public void setOnAdopt(AdoptListener listener) {
        this.onAdopt = listener == null ? editor -> { } : listener;
    }

    public void setOnSaveSkillFile(SaveSkillFileListener listener) {
        this.onSaveSkillFile = listener == null ? (editor, file) -> { } : listener;
    }

    public void setOnPropagateSkillFile(PropagateSkillFileListener listener) {
        this.onPropagateSkillFile = listener == null ? (editor, file) -> { } : listener;
    }

    public EditorModel editor() {
        return editor;
    }

    public SkillEditorModel skillEditor() {
        return skillEditor;
    }

    public boolean isDirty() {
        if (isSkillMode && skillEditor != null) {
            return skillEditor.isDirty();
        }
        return editor != null && editor.isDirty();
    }

    public void setEditorBody(String newBody) {
        if (editor != null) {
            editor.setBody(newBody);
        }
    }

    public void showEmpty() {
        editor = null;
        skillEditor = null;
        isSkillMode = false;
        title.setText("Selecciona un artefacto");
        hideNotice();
        metaBox.getChildren().setAll(hint("Elige un agente, una skill o .sdd en el árbol para verlo aquí."));
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(bodySection);
        hide(actionBar);
    }

    public void showLoading(String name) {
        editor = null;
        skillEditor = null;
        isSkillMode = false;
        title.setText(name);
        hideNotice();
        metaBox.getChildren().setAll(hint("Cargando…"));
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(bodySection);
        hide(actionBar);
    }

    public void showArtifact(ArtifactPreview preview, EditorModel editor) {
        this.editor = editor;
        this.skillEditor = null;
        this.isSkillMode = false;
        this.status = preview.status() == null ? "" : preview.status();
        this.headerLanguage = preview.headerLanguage();
        this.bodyLanguage = preview.bodyLanguage();

        title.setText(preview.name());
        hideNotice();
        showMeta(preview);
        showHeader();
        showSharedBody();
        showSkillFiles(preview);
        showBody();

        saveStatus.setText("");
        saveButton.setDisable(false);
        updateAdoptButton();
        updateDirty();
        show(actionBar);
    }

    public void showSkill(ArtifactPreview preview, SkillEditorModel skillEditor) {
        this.editor = null;
        this.skillEditor = skillEditor;
        this.isSkillMode = true;
        this.status = preview.status() == null ? "" : preview.status();

        title.setText(preview.name() + " (skill)");
        hideNotice();
        showMeta(preview);
        showSkillEditor();

        saveStatus.setText("");
        saveButton.setDisable(true); // Ocultar, usamos saveSkillFileButton
        updateAdoptButton();
        updateDirty();
        show(actionBar);
    }

    private void showSkillEditor() {
        if (skillEditor == null || skillEditor.files().isEmpty()) {
            hide(skillFilesSection);
            return;
        }

        updatingFromModel = true;
        skillFileChoice.getItems().setAll(skillEditor.files());
        String firstFile = skillEditor.selectedFile();
        skillFileChoice.getSelectionModel().select(firstFile);
        skillFileEdit.setText(skillEditor.getContent(firstFile));
        updatingFromModel = false;

        show(skillFilesSection);
    }

    private void updateAdoptButton() {
        boolean adoptable = "legacy".equals(status) || "detached".equals(status);
        adoptButton.setManaged(adoptable);
        adoptButton.setVisible(adoptable);
    }

    public void showSddDirectory(List<Path> files) {
        editor = null;
        skillEditor = null;
        isSkillMode = false;
        title.setText(".sdd");
        hideNotice();
        metaBox.getChildren().clear();
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(bodySection);
        hide(actionBar);

        // Mostrar lista de ficheros .sdd
        // (implementación existente)
    }

    public void showSddFile(String name, String content) {
        editor = null;
        skillEditor = null;
        isSkillMode = false;
        title.setText(name);
        showNotice("Solo lectura · fichero de .sdd");
        metaBox.getChildren().clear();
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(bodySection);
        hide(actionBar);
        showBodyContent(content, languageFor(name), false);
    }

    public void showError(String heading, String message) {
        editor = null;
        skillEditor = null;
        isSkillMode = false;
        title.setText(heading);
        showNotice("No se pudo previsualizar: " + message);
        metaBox.getChildren().clear();
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(bodySection);
        hide(actionBar);
    }

    public void showSaveStatus(String message, boolean error) {
        saveStatus.setText(message);
        saveStatus.getStyleClass().removeAll("ok", "error");
        saveStatus.getStyleClass().add(error ? "error" : "ok");
        updateDirty();
    }

    private void showMeta(ArtifactPreview preview) {
        metaBox.getChildren().clear();
        metaBox.getChildren().add(metaRow("Tipo", preview.kind()));
        metaBox.getChildren().add(metaRow("Proveedor", preview.provider()));
        metaBox.getChildren().add(metaRow("Ámbito", preview.scope()));
        if (preview.module() != null) {
            metaBox.getChildren().add(metaRow("Módulo", preview.module()));
        }
        if (preview.format() != null) {
            metaBox.getChildren().add(metaRow("Formato", preview.format()));
        }
        if (preview.status() != null) {
            metaBox.getChildren().add(metaRow("Estado", preview.status()));
        }
        if (preview.modifiedExternally()) {
            metaBox.getChildren().add(metaRow("Modificado fuera", "sí"));
        }
        metaBox.getChildren().add(metaRow("Ruta", preview.path()));
    }

    private void showHeader() {
        if (editor == null || !editor.headerEditable()) {
            hide(headerSection);
            return;
        }
        String languageName = CodeHighlighter.YAML.equals(headerLanguage) ? "YAML" : "TOML";
        headerTitle.setText("Cabecera (" + languageName + ")");
        updatingFromModel = true;
        headerEdit.setText(editor.header());
        updatingFromModel = false;
        headerEdit.setEditable(true);
        headerSection.getChildren().setAll(headerTitle, headerEdit);
        show(headerSection);
    }

    private void showSharedBody() {
        if (editor == null || !editor.hasGroup()) {
            hide(sharedSection);
            return;
        }
        sharedTitle.setText(editor.hasPlaceholder()
            ? "Cuerpo compartido (propagación · placeholder " + SharedBody.MODULE_PLACEHOLDER + ")"
            : "Cuerpo compartido (propagación)");
        updatingFromModel = true;
        sharedEdit.setText(editor.sharedBody());
        updatingFromModel = false;
        sharedResolved.setSelected(false);
        sharedResolved.setManaged(editor.hasPlaceholder());
        sharedResolved.setVisible(editor.hasPlaceholder());
        applySharedView();
        show(sharedSection);
    }

    private void applySharedView() {
        if (editor == null) {
            return;
        }
        boolean resolved = sharedResolved.isSelected();
        sharedEdit.setVisible(!resolved);
        sharedEdit.setManaged(!resolved);
        sharedView.setVisible(resolved);
        sharedView.setManaged(resolved);
        if (resolved) {
            sharedView.getEngine().loadContent(
                SharedBody.toDocument(editor.sharedBody(), editor.module(), true));
        }
    }

    private void showSkillFiles(ArtifactPreview preview) {
        // Solo se usa para mostrar información, la edición se hace en showSkillEditor
        hide(skillFilesSection);
    }

    private void showBody() {
        if (editor == null || !editor.bodyEditable() || editor.body().isBlank()) {
            hide(bodySection);
            return;
        }
        showBodyContent(editor.body(), bodyLanguage, true);
    }

    private void showBodyContent(String text, String language, boolean editable) {
        this.bodyLanguage = language;
        updatingFromModel = true;
        bodyEdit.setText(text);
        updatingFromModel = false;
        bodyEdit.setEditable(editable);
        boolean renderable = isRenderable(language);
        renderToggle.setDisable(!renderable);
        if (!renderable) {
            textToggle.setSelected(true);
        }
        applyBodyView();
        show(bodySection);
    }

    private void updateDirty() {
        boolean anyDirty;
        if (isSkillMode && skillEditor != null) {
            anyDirty = skillEditor.isDirty();
            saveSkillFileButton.setDisable(!skillEditor.isDirty(skillEditor.selectedFile()));
        } else {
            anyDirty = editor != null && editor.isDirty();
            boolean instanceDirty = editor != null && (editor.headerDirty() || editor.bodyDirty());
            saveButton.setDisable(!instanceDirty);
        }
        dirtyLabel.setText(anyDirty ? "Cambios sin guardar" : "");
    }

    private void applyBodyView() {
        boolean showRendered = renderToggle.isSelected() && !renderToggle.isDisabled();
        bodyEdit.setVisible(!showRendered);
        bodyEdit.setManaged(!showRendered);
        renderView.setVisible(showRendered);
        renderView.setManaged(showRendered);
        if (showRendered) {
            loadRender(bodyEdit.getText(), bodyLanguage);
        }
    }

    private void loadRender(String body, String language) {
        if ("markdown".equals(language)) {
            renderView.getEngine().loadContent(markdown.toDocument(body));
        } else {
            renderView.getEngine().loadContent(CodeHighlighter.toDocument(body, language));
        }
    }

    private static boolean isRenderable(String language) {
        return "markdown".equals(language)
            || CodeHighlighter.TOML.equals(language)
            || CodeHighlighter.YAML.equals(language);
    }

    private static String languageFor(String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase();
        return name.endsWith(".toml") ? CodeHighlighter.TOML : "markdown";
    }

    private static Node hint(String text) {
        Label hint = new Label(text);
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        return hint;
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private static HBox metaRow(String key, String value) {
        Label keyLabel = new Label(key + ":");
        keyLabel.getStyleClass().add("meta-key");
        Label valueLabel = new Label(value == null ? "-" : value);
        valueLabel.getStyleClass().add("meta-value");
        valueLabel.setWrapText(true);
        HBox row = new HBox(6, keyLabel, valueLabel);
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        return row;
    }

    private void showNotice(String text) {
        notice.setText(text);
        notice.setManaged(true);
        notice.setVisible(true);
    }

    private void hideNotice() {
        notice.setText("");
        notice.setManaged(false);
        notice.setVisible(false);
    }

    private static void show(Node node) {
        node.setManaged(true);
        node.setVisible(true);
    }

    private static void hide(Node node) {
        node.setManaged(false);
        node.setVisible(false);
    }
}
