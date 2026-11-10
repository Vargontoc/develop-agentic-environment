package es.vargontoc.agentic.viewer.preview;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import es.vargontoc.agentic.viewer.editor.EditorModel;
import es.vargontoc.agentic.viewer.editor.SharedBody;
import es.vargontoc.agentic.viewer.editor.SkillEditorModel;
import es.vargontoc.agentic.viewer.ui.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

    /** Acción de guardar todos los ficheros modificados de una skill. */
    public interface SaveAllSkillFilesListener {
        void onSaveAllSkillFiles(SkillEditorModel editor);
    }

    private final MarkdownRenderer markdown = new MarkdownRenderer();

    private Consumer<Path> onSddFileSelected = file -> { };
    private SaveListener onSave = editor -> { };
    private PropagateListener onPropagate = editor -> { };
    private AdoptListener onAdopt = editor -> { };
    private SaveSkillFileListener onSaveSkillFile = (editor, file) -> { };
    private PropagateSkillFileListener onPropagateSkillFile = (editor, file) -> { };
    private SaveAllSkillFilesListener onSaveAllSkillFiles = editor -> { };
    private Runnable onDirtyChanged = () -> { };

    // Estado para agentes
    private EditorModel editor;
    private String status = "";
    private String headerLanguage = "";
    private String bodyLanguage = "";
    private boolean updatingFromModel = false;
    private boolean lastDirtyNotified = false;

    // Estado para skills
    private SkillEditorModel skillEditor;
    private boolean isSkillMode = false;

    private final Label title = new Label("Nada seleccionado");
    private final Label notice = new Label();
    private final VBox metaBox = new VBox(3);

    // Sección de ficheros .sdd
    private final VBox sddSection = new VBox(8);
    private final Label sddTitle = new Label("Ficheros de .sdd");
    private final VBox sddList = new VBox(2);
    private final ScrollPane sddScroll = new ScrollPane();

    // Sección de cabecera (solo agentes)
    private final VBox headerSection = new VBox(6);
    private final Label headerTitle = new Label();
    private final TextArea headerEdit = new TextArea();

    // Sección de ficheros de skill
    private final VBox skillFilesSection = new VBox(8);
    private final ChoiceBox<String> skillFileChoice = new ChoiceBox<>();
    private final TextArea skillFileEdit = new TextArea();
    private final Button saveSkillFileButton = new Button("Guardar fichero");
    private final Button propagateSkillFileButton = new Button("Propagar");

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
    private final HBox actionBar = new HBox(10);

    public PreviewPanel() {
        getStyleClass().add("preview-panel");
        setPadding(new Insets(16));
        setSpacing(12);

        title.getStyleClass().add("panel-title");
        title.setWrapText(true);

        notice.getStyleClass().add("notice");
        notice.setWrapText(true);
        notice.setManaged(false);
        notice.setVisible(false);

        setupHeaderSection();
        setupSkillFilesSection();
        setupSharedSection();
        setupBodySection();
        setupSddSection();
        setupActionButtons();

        VBox.setVgrow(metaBox, Priority.NEVER);
        getChildren().setAll(title, notice, metaBox, sddSection, headerSection, skillFilesSection,
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
        Label fileLabel = new Label("Fichero");
        fileLabel.getStyleClass().add("form-label");

        skillFileChoice.setPrefWidth(240);
        skillFileChoice.setMaxWidth(Double.MAX_VALUE);
        skillFileEdit.setWrapText(false);
        skillFileEdit.getStyleClass().add("editor");
        VBox.setVgrow(skillFileEdit, Priority.ALWAYS);

        skillFileChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (skillEditor != null && newVal != null && !updatingFromModel) {
                if (oldVal != null) {
                    skillEditor.setContent(oldVal, skillFileEdit.getText());
                }
                skillEditor.selectFile(newVal);
                updatingFromModel = true;
                skillFileEdit.setText(skillEditor.getContent(newVal));
                updatingFromModel = false;
                updateDirty();
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

        saveSkillFileButton.getStyleClass().add("primary");
        saveSkillFileButton.setGraphic(Icons.of(Icons.Icon.SAVE, 14, "button-icon"));
        saveSkillFileButton.setTooltip(new Tooltip("Guarda el fichero seleccionado"));
        saveSkillFileButton.setOnAction(event -> {
            if (skillEditor != null) {
                String selected = skillEditor.selectedFile();
                if (selected != null && skillEditor.isDirty(selected)) {
                    onSaveSkillFile.onSaveSkillFile(skillEditor, selected);
                }
            }
        });

        propagateSkillFileButton.getStyleClass().add("secondary");
        propagateSkillFileButton.setGraphic(Icons.of(Icons.Icon.REFRESH, 14, "button-icon"));
        propagateSkillFileButton.setTooltip(new Tooltip("Propaga este fichero al resto de copias del grupo"));
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

        skillFilesSection.getChildren().setAll(fileLabel, skillFileChoice, skillFileEdit, skillActions);
        VBox.setVgrow(skillFilesSection, Priority.ALWAYS);
    }

    private void setupSharedSection() {
        sharedTitle.getStyleClass().add("section-title");
        sharedEdit.setWrapText(false);
        sharedEdit.getStyleClass().add("editor");
        sharedEdit.setPrefRowCount(7);
        sharedView.setPrefHeight(150);
        sharedView.setMinHeight(110);
        sharedView.getStyleClass().add("render-view");
        sharedResolved.selectedProperty().addListener((observable, previous, selected) -> applySharedView());

        propagateButton.getStyleClass().add("secondary");
        propagateButton.setGraphic(Icons.of(Icons.Icon.REFRESH, 14, "button-icon"));
        HBox sharedActions = new HBox(8, propagateButton, sharedResolved);
        sharedActions.setAlignment(Pos.CENTER_LEFT);
        StackPane sharedStack = new StackPane(sharedEdit, sharedView);
        VBox.setVgrow(sharedStack, Priority.ALWAYS);
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
        textToggle.getStyleClass().add("segment");
        renderToggle.getStyleClass().add("segment");
        group.selectedToggleProperty().addListener((observable, previous, selected) -> applyBodyView());

        HBox toggleRow = new HBox(6, textToggle, renderToggle);
        toggleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane bodyStack = new StackPane(bodyEdit, renderView);
        renderView.getStyleClass().add("render-view");
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

    private void setupSddSection() {
        sddTitle.getStyleClass().add("section-title");
        Label hint = new Label("Solo lectura · selecciona un fichero para verlo");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        sddList.setPadding(new Insets(2, 0, 0, 0));
        sddScroll.setContent(sddList);
        sddScroll.setFitToWidth(true);
        sddScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sddScroll.getStyleClass().add("sdd-scroll");
        VBox.setVgrow(sddScroll, Priority.ALWAYS);
        sddSection.getChildren().setAll(sddTitle, hint, sddScroll);
        VBox.setVgrow(sddSection, Priority.ALWAYS);
    }

    private void showSddSectionFull() {
        VBox.setVgrow(sddSection, Priority.ALWAYS);
        sddScroll.setPrefViewportHeight(320);
        sddScroll.setMaxHeight(Double.MAX_VALUE);
        show(sddSection);
    }

    private void showSddSectionCompact() {
        if (sddList.getChildren().isEmpty()) {
            hide(sddSection);
            return;
        }
        VBox.setVgrow(sddSection, Priority.NEVER);
        sddScroll.setPrefViewportHeight(120);
        sddScroll.setMaxHeight(150);
        show(sddSection);
    }

    private void setupActionButtons() {
        propagateButton.setOnAction(event -> {
            if (editor != null && editor.hasGroup()) {
                onPropagate.onPropagate(editor);
            }
        });

        adoptButton.getStyleClass().add("secondary");
        adoptButton.setGraphic(Icons.of(Icons.Icon.LINK, 14, "button-icon"));
        adoptButton.setOnAction(event -> {
            if (editor != null) {
                onAdopt.onAdopt(editor);
            }
        });
        adoptButton.setManaged(false);
        adoptButton.setVisible(false);

        saveButton.getStyleClass().add("primary");
        saveButton.setGraphic(Icons.of(Icons.Icon.SAVE, 14, "button-icon"));
        saveButton.setTooltip(new Tooltip("Guardar (Ctrl+S)"));
        saveButton.setOnAction(event -> {
            if (isSkillMode) {
                if (skillEditor != null && skillEditor.isDirty()) {
                    onSaveAllSkillFiles.onSaveAllSkillFiles(skillEditor);
                }
                return;
            }
            if (editor != null && editor.isDirty()) {
                onSave.onSave(editor);
            }
        });
        saveButton.setDisable(true);

        dirtyLabel.getStyleClass().add("warning");
        dirtyLabel.setWrapText(true);
        saveStatus.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actionBar.getChildren().setAll(saveButton, propagateButton, adoptButton, spacer, dirtyLabel, saveStatus);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.getStyleClass().add("action-bar");
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

    public void setOnSaveAllSkillFiles(SaveAllSkillFilesListener listener) {
        this.onSaveAllSkillFiles = listener == null ? editor -> { } : listener;
    }

    /** Se invoca solo cuando el panel pasa de limpio a sucio (o al contrario). */
    public void setOnDirtyChanged(Runnable listener) {
        this.onDirtyChanged = listener == null ? () -> { } : listener;
    }

    public EditorModel editor() {
        return editor;
    }

    public SkillEditorModel skillEditor() {
        return skillEditor;
    }

    public boolean isSkillMode() {
        return isSkillMode;
    }

    public boolean isDirty() {
        if (isSkillMode && skillEditor != null) {
            return skillEditor.isDirty();
        }
        return editor != null && editor.isDirty();
    }

    /** Ruta del artefacto abierto, sea agente o skill. */
    public Path currentPath() {
        if (isSkillMode && skillEditor != null) {
            return skillEditor.skillPath();
        }
        return editor == null ? null : editor.path();
    }

    /** Indica si el nodo del árbol con esta ruta tiene cambios sin guardar. */
    public boolean isDirtyNode(String nodePath) {
        if (!isDirty() || nodePath == null) {
            return false;
        }
        Path current = currentPath();
        if (current == null) {
            return false;
        }
        try {
            return current.toAbsolutePath().normalize()
                .equals(Path.of(nodePath).toAbsolutePath().normalize());
        } catch (RuntimeException exception) {
            return false;
        }
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
        title.setText("Nada seleccionado");
        setTitleIcon(Icons.Icon.SEARCH, "icon-search");
        hideNotice();
        setMetaMode(true);
        metaBox.getChildren().setAll(emptyState());
        hide(sddSection);
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(bodySection);
        hide(actionBar);
        notifyDirtyChanged();
    }

    public void showLoading(String name) {
        editor = null;
        skillEditor = null;
        isSkillMode = false;
        title.setText(name);
        setTitleIcon(Icons.Icon.CLOCK, "icon-clock");
        hideNotice();
        setMetaMode(false);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(18, 18);
        spinner.setMaxSize(18, 18);
        Label label = new Label("Cargando…");
        label.getStyleClass().add("muted");
        HBox row = new HBox(10, spinner, label);
        row.setAlignment(Pos.CENTER_LEFT);
        metaBox.getChildren().setAll(row);
        hide(sddSection);
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(bodySection);
        hide(actionBar);
        notifyDirtyChanged();
    }

    public void showArtifact(ArtifactPreview preview, EditorModel editor) {
        this.editor = editor;
        this.skillEditor = null;
        this.isSkillMode = false;
        this.status = preview.status() == null ? "" : preview.status();
        this.headerLanguage = preview.headerLanguage();
        this.bodyLanguage = preview.bodyLanguage();

        title.setText(preview.name());
        String kind = preview.kind() == null ? "" : preview.kind();
        Icons.Icon titleIcon = switch (kind) {
            case "agent" -> Icons.Icon.AGENT;
            case "skill" -> Icons.Icon.SKILL;
            case "profile" -> Icons.Icon.PROFILE;
            default -> Icons.Icon.FILE_TEXT;
        };
        setTitleIcon(titleIcon, "icon-" + kind);
        hideNotice();
        setMetaMode(false);
        showMeta(preview);
        hide(sddSection);
        hide(skillFilesSection);
        showHeader();
        showSharedBody();
        showBody();

        saveStatus.setText("");
        saveButton.setText("Guardar");
        saveButton.setDisable(true);
        propagateButton.setManaged(editor.hasGroup());
        propagateButton.setVisible(editor.hasGroup());
        updateAdoptButton();
        updateDirty();
        show(actionBar);
    }

    public void showSkill(ArtifactPreview preview, SkillEditorModel skillEditor) {
        this.editor = null;
        this.skillEditor = skillEditor;
        this.isSkillMode = true;
        this.status = preview.status() == null ? "" : preview.status();

        title.setText(preview.name() + " · skill");
        setTitleIcon(Icons.Icon.SKILL, "icon-skill");
        hideNotice();
        setMetaMode(false);
        showMeta(preview);
        hide(sddSection);
        hide(headerSection);
        hide(sharedSection);
        hide(bodySection);
        showSkillEditor();

        saveStatus.setText("");
        saveButton.setText("Guardar todo");
        saveButton.setDisable(true);
        propagateButton.setManaged(false);
        propagateButton.setVisible(false);
        adoptButton.setManaged(false);
        adoptButton.setVisible(false);
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
        String selected = skillEditor.selectedFile();
        skillFileChoice.getSelectionModel().select(selected);
        skillFileEdit.setText(skillEditor.getContent(selected));
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
        setTitleIcon(Icons.Icon.SDD, "icon-sdd");
        hideNotice();
        setMetaMode(false);
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(bodySection);
        hide(actionBar);

        sddTitle.setText("Ficheros de .sdd (" + files.size() + ")");
        sddList.getChildren().clear();
        if (files.isEmpty()) {
            Label empty = new Label("No hay ficheros .md ni .toml en .sdd.");
            empty.getStyleClass().add("muted");
            empty.setWrapText(true);
            sddList.getChildren().add(empty);
        } else {
            for (Path file : files) {
                Hyperlink link = new Hyperlink(file.getFileName().toString());
                link.getStyleClass().add("file-link");
                link.setGraphic(Icons.of(Icons.Icon.FILE_TEXT, 14, "file-icon"));
                link.setTooltip(new Tooltip(file.toString()));
                link.setOnAction(event -> onSddFileSelected.accept(file));
                sddList.getChildren().add(link);
            }
        }

        metaBox.getChildren().setAll(hint("Especificaciones del proyecto en solo lectura."));
        showSddSectionFull();
        notifyDirtyChanged();
    }

    public void showSddFile(String name, String content) {
        editor = null;
        skillEditor = null;
        isSkillMode = false;
        title.setText(name);
        setTitleIcon(Icons.Icon.FILE_TEXT, "icon-sdd");
        showNotice("Solo lectura · fichero de .sdd", false);
        setMetaMode(false);
        metaBox.getChildren().clear();
        showSddSectionCompact();
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(actionBar);
        showBodyContent(content, languageFor(name), false);
        notifyDirtyChanged();
    }

    public void showError(String heading, String message) {
        editor = null;
        skillEditor = null;
        isSkillMode = false;
        title.setText(heading);
        setTitleIcon(Icons.Icon.ALERT, "icon-error");
        showNotice("No se pudo previsualizar: " + message, true);
        setMetaMode(false);
        metaBox.getChildren().clear();
        hide(sddSection);
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(bodySection);
        hide(actionBar);
        notifyDirtyChanged();
    }

    /** Muestra una explicación para nodos que no son artefactos editables. */
    public void showMessage(String heading, String message) {
        editor = null;
        skillEditor = null;
        isSkillMode = false;
        title.setText(heading);
        setTitleIcon(Icons.Icon.INFO, "icon-info");
        hideNotice();
        setMetaMode(true);
        Label text = new Label(message);
        text.getStyleClass().add("muted");
        text.setWrapText(true);
        text.setMaxWidth(620);
        VBox box = new VBox(8, text);
        box.getStyleClass().add("empty-state");
        box.setPadding(new Insets(24));
        metaBox.getChildren().setAll(box);
        hide(sddSection);
        hide(headerSection);
        hide(skillFilesSection);
        hide(sharedSection);
        hide(bodySection);
        hide(actionBar);
        notifyDirtyChanged();
    }

    public void showSaveStatus(String message, boolean error) {
        saveStatus.setText(message);
        saveStatus.getStyleClass().removeAll("ok", "error");
        saveStatus.getStyleClass().add(error ? "error" : "ok");
        updateDirty();
    }

    private void showMeta(ArtifactPreview preview) {
        metaBox.getChildren().clear();
        metaBox.getChildren().add(metaRow("Tipo", kindLabel(preview.kind())));
        metaBox.getChildren().add(metaRow("Proveedor", preview.provider()));
        metaBox.getChildren().add(metaRow("Ámbito", scopeLabel(preview.scope())));
        if (preview.module() != null) {
            metaBox.getChildren().add(metaRow("Módulo", preview.module()));
        }
        if (preview.format() != null) {
            metaBox.getChildren().add(metaRow("Formato", formatLabel(preview.format())));
        }
        if (preview.status() != null) {
            metaBox.getChildren().add(metaRow("Estado", statusLabel(preview.status())));
        }
        if (preview.modifiedExternally()) {
            metaBox.getChildren().add(metaRow("Modificado fuera", "Sí"));
        }
        metaBox.getChildren().add(metaRow("Ruta", preview.path()));
    }

    private void setMetaMode(boolean fill) {
        VBox.setVgrow(metaBox, fill ? Priority.ALWAYS : Priority.NEVER);
        metaBox.setAlignment(fill ? Pos.CENTER : Pos.TOP_LEFT);
    }

    private static String kindLabel(String kind) {
        if (kind == null) {
            return "—";
        }
        return switch (kind) {
            case "agent" -> "Agente";
            case "skill" -> "Skill";
            case "profile" -> "Perfil";
            default -> kind;
        };
    }

    private static String scopeLabel(String scope) {
        if (scope == null) {
            return "—";
        }
        return switch (scope) {
            case "global" -> "Global";
            case "project" -> "Proyecto";
            default -> scope;
        };
    }

    private static String statusLabel(String status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case "linked" -> "Vinculado";
            case "legacy" -> "Personalizado";
            case "detached" -> "Desvinculado";
            default -> status;
        };
    }

    private static String formatLabel(String format) {
        if (format == null) {
            return "—";
        }
        return switch (format) {
            case "markdown" -> "Markdown";
            case "codex_toml" -> "TOML";
            default -> format;
        };
    }

    private void showHeader() {
        if (editor == null || !editor.headerEditable()) {
            hide(headerSection);
            return;
        }
        String languageName = CodeHighlighter.YAML.equals(headerLanguage) ? "YAML" : "TOML";
        headerTitle.setText("Cabecera · " + languageName);
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
            ? "Cuerpo compartido · placeholder " + SharedBody.MODULE_PLACEHOLDER
            : "Cuerpo compartido");
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
            saveButton.setDisable(!anyDirty);
            String selected = skillEditor.selectedFile();
            saveSkillFileButton.setDisable(selected == null || !skillEditor.isDirty(selected));
            long dirtyFiles = skillEditor.files().stream().filter(skillEditor::isDirty).count();
            dirtyLabel.setText(anyDirty
                ? "Cambios sin guardar" + (dirtyFiles > 1 ? " · " + dirtyFiles + " ficheros" : "")
                : "");
        } else {
            anyDirty = editor != null && editor.isDirty();
            boolean instanceDirty = editor != null && (editor.headerDirty() || editor.bodyDirty());
            saveButton.setDisable(!instanceDirty);
            dirtyLabel.setText(anyDirty ? "Cambios sin guardar" : "");
        }
        if (anyDirty != lastDirtyNotified) {
            lastDirtyNotified = anyDirty;
            onDirtyChanged.run();
        }
    }

    private void notifyDirtyChanged() {
        lastDirtyNotified = isDirty();
        onDirtyChanged.run();
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

    private static VBox emptyState() {
        Label heading = new Label("Elige un artefacto para empezar");
        heading.getStyleClass().add("empty-title");

        Label text = new Label(
            "Selecciona un agente, una skill o el directorio .sdd en el árbol de la izquierda. "
                + "Los cambios se guardan con el CLI, nunca directamente sobre los ficheros.");
        text.getStyleClass().add("muted");
        text.setWrapText(true);
        text.setMaxWidth(620);

        Label keys = new Label("Ctrl+F buscar · F5 recargar · Ctrl+S guardar · F6 consola");
        keys.getStyleClass().add("key-hint");
        keys.setWrapText(true);

        VBox box = new VBox(8, Icons.of(Icons.Icon.SEARCH, 28, "empty-icon"), heading, text, keys);
        box.getStyleClass().add("empty-state");
        box.setPadding(new Insets(24));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private void setTitleIcon(Icons.Icon icon, String colorClass) {
        title.setGraphic(icon == null
            ? null
            : Icons.of(icon, 22, "panel-title-icon", colorClass));
    }

    private static Node hint(String text) {
        Label hint = new Label(text);
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        return hint;
    }

    private static HBox metaRow(String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("meta-key");
        Label valueLabel = new Label(value == null ? "—" : value);
        valueLabel.getStyleClass().add("meta-value");
        valueLabel.setWrapText(true);
        HBox row = new HBox(10, keyLabel, valueLabel);
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        return row;
    }

    private void showNotice(String text, boolean error) {
        notice.setText(text);
        notice.getStyleClass().removeAll("error", "warning");
        notice.getStyleClass().add(error ? "error" : "warning");
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
