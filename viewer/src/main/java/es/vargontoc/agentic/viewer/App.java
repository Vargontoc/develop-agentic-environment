package es.vargontoc.agentic.viewer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.agentic.viewer.cli.CliClient;
import es.vargontoc.agentic.viewer.cli.CliClientException;
import es.vargontoc.agentic.viewer.cli.CliCommand;
import es.vargontoc.agentic.viewer.cli.CliLocator;
import es.vargontoc.agentic.viewer.cli.CliProcess;
import es.vargontoc.agentic.viewer.cli.CliProcess.CliResult;
import es.vargontoc.agentic.viewer.cli.ErrorPresenter;
import es.vargontoc.agentic.viewer.cli.ErrorPresenter.ErrorNotice;
import es.vargontoc.agentic.viewer.cli.model.AdoptData;
import es.vargontoc.agentic.viewer.cli.model.CatalogData;
import es.vargontoc.agentic.viewer.cli.model.CreateArtifactData;
import es.vargontoc.agentic.viewer.cli.model.CreateModuleData;
import es.vargontoc.agentic.viewer.cli.model.DescribeData;
import es.vargontoc.agentic.viewer.cli.model.InitData;
import es.vargontoc.agentic.viewer.cli.model.SaveData;
import es.vargontoc.agentic.viewer.cli.model.StatusData;
import es.vargontoc.agentic.viewer.cli.model.TreeData;
import es.vargontoc.agentic.viewer.creation.AddTechnicianDialog;
import es.vargontoc.agentic.viewer.creation.CreateArtifactDialog;
import es.vargontoc.agentic.viewer.creation.CreateModuleDialog;
import es.vargontoc.agentic.viewer.creation.CreationPreviewDialog;
import es.vargontoc.agentic.viewer.creation.EditTemplateDialog;
import es.vargontoc.agentic.viewer.diff.DiffReview;
import es.vargontoc.agentic.viewer.diff.DiffReviewDialog;
import es.vargontoc.agentic.viewer.editor.EditorModel;
import es.vargontoc.agentic.viewer.editor.SkillEditorModel;
import es.vargontoc.agentic.viewer.preview.ArtifactPreview;
import es.vargontoc.agentic.viewer.preview.PreviewPanel;
import es.vargontoc.agentic.viewer.preview.SddFiles;
import es.vargontoc.agentic.viewer.terminal.PtyConsolePanel;
import es.vargontoc.agentic.viewer.tree.TreeLabels;
import es.vargontoc.agentic.viewer.tree.TreeModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * Viewer JavaFX: árbol lateral, sesión de proyecto, estado y herramientas del
 * CLI. TASK-005 añade la navegación Global/Project.
 */
public final class App extends Application {

    private static final String[] TREE_CLASSES = {
        "tree-root", "tree-provider", "tree-agent", "tree-skill", "tree-profile",
        "tree-sdd", "tree-module", "tree-technician", "tree-pending", "tree-legacy", "tree-detached"
    };

    private Stage stage;
    private CliCommand cliCommand;
    private CliClient cliClient;
    private ProjectSession projectSession;
    private String cliError;

    private Label projectPath;
    private Label projectState;
    private Label projectSummary;
    private Button selectProjectButton;
    private Button initProjectButton;
    private Button createModuleButton;
    private Button addTechnicianButton;
    private Button createArtifactButton;

    private TreeView<TreeData.Node> treeView;
    private TreeItem<TreeData.Node> treeRoot;
    private Label treeWarnings;
    private Label selectionInfo;
    private PreviewPanel previewPanel;
    private PtyConsolePanel ptyConsolePanel;
    private boolean suppressSelection;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        try {
            cliCommand = CliLocator.resolve(Path.of("").toAbsolutePath());
            cliClient = new CliClient(new CliProcess(cliCommand), new ObjectMapper());
            projectSession = new ProjectSession(cliClient);
        } catch (RuntimeException exception) {
            cliError = exception.getMessage();
        }

        Label title = new Label("Agentic Viewer");
        title.getStyleClass().add("title");

        Label subtitle = new Label("JavaFX · TASK-013: consola PTY con pty4j");
        subtitle.getStyleClass().add("subtitle");

        VBox header = new VBox(4, title, subtitle);

        previewPanel = new PreviewPanel();
        previewPanel.setOnSddFileSelected(this::loadSddFile);
        previewPanel.setOnSave(this::saveEditorAsync);
        previewPanel.setOnPropagate(this::propagate);
        previewPanel.setOnAdopt(this::adopt);
        previewPanel.setOnSaveSkillFile(this::saveSkillFile);
        previewPanel.setOnPropagateSkillFile(this::propagateSkillFile);

        ptyConsolePanel = new PtyConsolePanel(cliCommand, this::currentProject);

        VBox main = new VBox(12, projectCard(), previewPanel, ptyConsoleSection());
        VBox.setVgrow(previewPanel, Priority.ALWAYS);

        SplitPane split = new SplitPane(sidebar(), main);
        split.setDividerPositions(0.30);

        VBox root = new VBox(20, header, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        root.getStyleClass().add("app");
        root.setPadding(new Insets(24));

        Scene scene = new Scene(root, 1180, 820);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("Agentic Viewer");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> {
            if (ptyConsolePanel != null) {
                ptyConsolePanel.shutdown();
            }
        });

        if (cliError != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(stage);
            alert.setTitle("Error de aplicación");
            alert.setHeaderText("No se encontró el CLI");
            alert.setContentText(cliError);
            alert.showAndWait();
        } else {
            refreshTree();
        }
    }

    private VBox sidebar() {
        Label heading = new Label("Navegación");
        heading.getStyleClass().add("heading");

        Button refresh = new Button("Refrescar");
        refresh.setDisable(cliClient == null);
        refresh.setOnAction(event -> refreshTree());

        HBox headingRow = new HBox(8, heading, refresh);
        headingRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(heading, Priority.ALWAYS);

        treeView = new TreeView<>();
        treeRoot = new TreeItem<>();
        treeRoot.setExpanded(true);
        treeView.setRoot(treeRoot);
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("tree-view");
        treeView.setCellFactory(view -> new TreeCell<>() {
            @Override
            protected void updateItem(TreeData.Node node, boolean empty) {
                super.updateItem(node, empty);
                getStyleClass().removeAll(TREE_CLASSES);
                if (empty || node == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(TreeLabels.label(node));
                getStyleClass().add("tree-" + node.type());
                if (node.pending()) {
                    getStyleClass().add("tree-pending");
                }
                if ("legacy".equals(node.status())) {
                    getStyleClass().add("tree-legacy");
                }
                if ("detached".equals(node.status())) {
                    getStyleClass().add("tree-detached");
                }
                setTooltip(node.path() == null ? null : new Tooltip(node.path()));
            }
        });
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (suppressSelection) {
                return;
            }
            TreeData.Node node = selected == null ? null : selected.getValue();
            if (!guardNavigation(previous, node)) {
                suppressSelection = true;
                treeView.getSelectionModel().select(previous);
                suppressSelection = false;
                return;
            }
            if (node == null) {
                selectionInfo.setText("");
            } else {
                selectionInfo.setText(formatNode(node));
            }
            onSelectNode(node);
        });

        treeWarnings = new Label(cliError == null ? "" : cliError);
        treeWarnings.getStyleClass().add(cliError == null ? "muted" : "error");
        treeWarnings.setWrapText(true);
        treeWarnings.setManaged(cliError != null);
        treeWarnings.setVisible(cliError != null);

        selectionInfo = new Label();
        selectionInfo.getStyleClass().add("selection");
        selectionInfo.setWrapText(true);

        VBox sidebar = new VBox(10, headingRow, treeView, treeWarnings, selectionInfo);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(12));
        sidebar.setPrefWidth(330);
        sidebar.setMinWidth(260);
        return sidebar;
    }

    private VBox projectCard() {
        Label heading = new Label("Proyecto");
        heading.getStyleClass().add("heading");

        projectPath = new Label("Ningún proyecto seleccionado");
        projectPath.getStyleClass().add("muted");
        projectPath.setWrapText(true);

        projectState = new Label("Selecciona una carpeta para empezar");
        projectState.getStyleClass().add("muted");
        projectState.setWrapText(true);

        projectSummary = new Label();
        projectSummary.getStyleClass().add("muted");
        projectSummary.setWrapText(true);

        selectProjectButton = new Button("Seleccionar proyecto");
        selectProjectButton.setOnAction(event -> chooseProject());

        initProjectButton = new Button("Iniciar proyecto");
        initProjectButton.setDisable(true);
        initProjectButton.setOnAction(event -> initProject());

        createModuleButton = new Button("Crear módulo");
        createModuleButton.setDisable(true);
        createModuleButton.setOnAction(event -> createModule());

        addTechnicianButton = new Button("Añadir técnico");
        addTechnicianButton.setDisable(true);
        addTechnicianButton.setOnAction(event -> addTechnician());

        createArtifactButton = new Button("Crear artefacto");
        createArtifactButton.setDisable(true);
        createArtifactButton.setOnAction(event -> createArtifact());

        boolean missing = projectSession == null;
        selectProjectButton.setDisable(missing);
        if (missing) {
            setProjectState(cliError == null ? "CLI no disponible" : cliError, "error");
        }

        HBox row1 = new HBox(8, selectProjectButton, initProjectButton);
        row1.setAlignment(Pos.CENTER_LEFT);

        HBox row2 = new HBox(8, createModuleButton, addTechnicianButton);
        row2.setAlignment(Pos.CENTER_LEFT);

        HBox row3 = new HBox(8, createArtifactButton);
        row3.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, heading, projectPath, projectState, projectSummary, row1, row2, row3);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        return card;
    }

    private VBox cliCard() {
        Label heading = new Label("CLI");
        heading.getStyleClass().add("heading");

        Label path = new Label(cliCommand == null
            ? "No se encontró el jar del CLI"
            : "Jar: " + cliCommand.cliJar());
        path.getStyleClass().add("muted");
        path.setWrapText(true);

        TextArea output = new TextArea();
        output.setEditable(false);
        output.setWrapText(false);
        output.setPrefRowCount(12);
        output.getStyleClass().add("console");

        Label state = new Label(cliError == null ? "Listo" : cliError);
        state.getStyleClass().add(cliError == null ? "muted" : "error");
        state.setWrapText(true);

        Button rawVersion = new Button("--version");
        Button status = new Button("status");
        Button clear = new Button("Limpiar");

        rawVersion.setOnAction(event -> runRaw(List.of("--version"), output, state));
        status.setOnAction(event -> runTyped("status",
            () -> formatStatus(cliClient.status(currentProject())), output, state));
        clear.setOnAction(event -> {
            output.clear();
            state.setText("Listo");
        });

        HBox primary = new HBox(8, rawVersion, status, clear);
        primary.setAlignment(Pos.CENTER_LEFT);

        boolean missing = cliClient == null;
        rawVersion.setDisable(missing);
        status.setDisable(missing);

        VBox card = new VBox(10, heading, path, primary, state, output);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        return card;
    }

    /** Sección de la consola PTY con botón para iniciar. */
    private VBox ptyConsoleSection() {
        Label heading = new Label("Consola PTY");
        heading.getStyleClass().add("heading");

        Label description = new Label("Ejecuta el CLI interactivo en una terminal integrada.");
        description.getStyleClass().add("muted");
        description.setWrapText(true);

        Button startButton = new Button("Iniciar consola");
        startButton.setDisable(cliCommand == null);
        startButton.setOnAction(event -> {
            if (!ptyConsolePanel.isActive()) {
                ptyConsolePanel.startSession();
            }
        });

        HBox headerRow = new HBox(8, heading, startButton);
        HBox.setHgrow(heading, Priority.ALWAYS);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox section = new VBox(8, headerRow, description, ptyConsolePanel);
        section.getStyleClass().add("card");
        section.setPadding(new Insets(16));
        section.setMinHeight(200);
        return section;
    }

    private void refreshTree() {
        if (cliClient == null) {
            return;
        }
        treeWarnings.setText("Cargando árbol…");
        treeWarnings.getStyleClass().removeAll("muted", "warning", "error");
        treeWarnings.getStyleClass().add("muted");
        treeWarnings.setManaged(true);
        treeWarnings.setVisible(true);

        Thread.ofVirtual().start(() -> {
            try {
                TreeData data = cliClient.tree(currentProject());
                Platform.runLater(() -> updateTree(data));
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> showTreeError(notice));
            } catch (RuntimeException exception) {
                Platform.runLater(() -> showTreeError(new ErrorNotice(
                    "client.unexpected", null, exception.getMessage(), "Reintentar.")));
            }
        });
    }

    private void updateTree(TreeData data) {
        treeRoot.getChildren().setAll(TreeModel.roots(data).stream().map(this::toItem).toList());
        String warnings = formatTreeWarnings(data);
        treeWarnings.setText(warnings);
        treeWarnings.setManaged(!warnings.isBlank());
        treeWarnings.setVisible(!warnings.isBlank());
        treeWarnings.getStyleClass().removeAll("muted", "warning", "error");
        treeWarnings.getStyleClass().add(warnings.isBlank() ? "muted" : "warning");
        selectionInfo.setText("");
    }

    private void showTreeError(ErrorNotice notice) {
        treeRoot.getChildren().clear();
        treeWarnings.setText("Error " + notice.code() + ": " + notice.message() + " " + notice.action());
        treeWarnings.setManaged(true);
        treeWarnings.setVisible(true);
        treeWarnings.getStyleClass().removeAll("muted", "warning", "error");
        treeWarnings.getStyleClass().add("error");
    }

    private TreeItem<TreeData.Node> toItem(TreeData.Node node) {
        TreeItem<TreeData.Node> item = new TreeItem<>(node);
        if (node.children() != null) {
            item.getChildren().setAll(node.children().stream().map(this::toItem).toList());
        }
        item.setExpanded("root".equals(node.type()));
        return item;
    }

    private void onSelectNode(TreeData.Node node) {
        if (node == null || node.type() == null) {
            previewPanel.showEmpty();
            return;
        }
        switch (node.type()) {
            case "agent", "skill", "profile" -> loadArtifact(node);
            case "sdd" -> loadSddDirectory(node);
            default -> previewPanel.showEmpty();
        }
    }

    private void loadArtifact(TreeData.Node node) {
        if (node.path() == null) {
            previewPanel.showError(node.name(), "El nodo no tiene una ruta en disco.");
            return;
        }
        Path artifact = Path.of(node.path());
        previewPanel.showLoading(node.name());
        Thread.ofVirtual().start(() -> {
            try {
                DescribeData data = cliClient.describe(artifact, currentProject());
                
                // Si es una skill, usar el editor de skills
                if ("skill".equals(data.kind()) && data.files() != null && !data.files().isEmpty()) {
                    loadSkill(data);
                } else {
                    EditorModel editor = EditorModel.of(data, currentProject());
                    Platform.runLater(() -> previewPanel.showArtifact(ArtifactPreview.of(data), editor));
                }
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> previewPanel.showError(
                    node.name(), notice.message() + " " + notice.action()));
            } catch (RuntimeException exception) {
                Platform.runLater(() -> previewPanel.showError(
                    node.name(), exception.getMessage() == null ? "Error inesperado." : exception.getMessage()));
            }
        });
    }

    private void loadSkill(DescribeData data) {
        try {
            // Cargar el contenido de todos los ficheros de la skill
            Map<String, String> contents = new HashMap<>();
            Path skillPath = Path.of(data.path());
            
            for (String file : data.files()) {
                Path filePath = skillPath.resolve(file);
                try {
                    String content = Files.readString(filePath);
                    contents.put(file, content);
                } catch (IOException e) {
                    contents.put(file, ""); // Si no se puede leer, dejar vacío
                }
            }
            
            SkillEditorModel skillEditor = new SkillEditorModel(data, currentProject(), contents);
            Platform.runLater(() -> previewPanel.showSkill(ArtifactPreview.of(data), skillEditor));
        } catch (RuntimeException exception) {
            Platform.runLater(() -> previewPanel.showError(
                data.name(), exception.getMessage() == null ? "Error inesperado." : exception.getMessage()));
        }
    }

    private void loadSddDirectory(TreeData.Node node) {
        Path sddDir = node.path() == null ? null : Path.of(node.path());
        try {
            previewPanel.showSddDirectory(SddFiles.list(sddDir));
        } catch (SddFiles.SddReadException exception) {
            previewPanel.showError(".sdd", exception.getMessage());
        }
    }

    private void loadSddFile(Path file) {
        Path sddDir = file.getParent();
        previewPanel.showLoading(file.getFileName().toString());
        Thread.ofVirtual().start(() -> {
            try {
                String content = SddFiles.read(sddDir, file);
                Platform.runLater(() -> previewPanel.showSddFile(file.getFileName().toString(), content));
            } catch (SddFiles.SddReadException exception) {
                Platform.runLater(() -> previewPanel.showError(
                    file.getFileName().toString(), exception.getMessage()));
            }
        });
    }

    private boolean guardNavigation(TreeItem<TreeData.Node> previous, TreeData.Node node) {
        // Verificar si hay cambios sin guardar en el editor de agentes
        EditorModel editor = previewPanel.editor();
        if (editor != null && editor.isDirty()) {
            return confirmSaveForAgent(editor);
        }
        
        // Verificar si hay cambios sin guardar en el editor de skills
        SkillEditorModel skillEditor = previewPanel.skillEditor();
        if (skillEditor != null && skillEditor.isDirty()) {
            return confirmSaveForSkill(skillEditor);
        }
        
        return true;
    }

    private boolean confirmSaveForAgent(EditorModel editor) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Cambios sin guardar");
        alert.setHeaderText("El artefacto tiene cambios sin guardar.");
        alert.setContentText("Elige qué hacer antes de cambiar de selección.");
        ButtonType saveButton = new ButtonType("Guardar");
        ButtonType discardButton = new ButtonType("Descartar");
        ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancelButton) {
            return false;
        }
        if (result.get() == saveButton) {
            SaveOutcome outcome = saveEditor(editor);
            applySaveOutcome(editor, outcome);
            if (!outcome.ok()) {
                return false;
            }
            if (editor.sharedBodyDirty() && !propagate(editor)) {
                return false;
            }
        }
        return true;
    }

    private boolean confirmSaveForSkill(SkillEditorModel skillEditor) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Cambios sin guardar");
        alert.setHeaderText("La skill tiene ficheros con cambios sin guardar.");
        alert.setContentText("¿Deseas guardar todos los ficheros modificados antes de cambiar?");
        ButtonType saveButton = new ButtonType("Guardar todo");
        ButtonType discardButton = new ButtonType("Descartar");
        ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancelButton) {
            return false;
        }
        if (result.get() == saveButton) {
            // Guardar todos los ficheros modificados
            for (String file : skillEditor.files()) {
                if (skillEditor.isDirty(file)) {
                    try {
                        String content = skillEditor.getContent(file);
                        cliClient.saveBody(skillEditor.skillPath(), content, skillEditor.project(), file);
                        skillEditor.markSaved(file);
                    } catch (CliClientException exception) {
                        ErrorNotice notice = ErrorPresenter.present(exception);
                        previewPanel.showSaveStatus("Error al guardar " + file + ": " + notice.message(), true);
                        return false;
                    }
                }
            }
            previewPanel.showSaveStatus("Todos los ficheros guardados.", false);
        }
        return true;
    }

    private void saveEditorAsync(EditorModel editor) {
        Thread.ofVirtual().start(() -> {
            SaveOutcome outcome = saveEditor(editor);
            Platform.runLater(() -> applySaveOutcome(editor, outcome));
        });
    }

    private SaveOutcome saveEditor(EditorModel editor) {
        String error = null;
        if (editor.headerDirty()) {
            error = saveHeaderEdit(editor);
        }
        if (error == null && editor.bodyDirty()) {
            error = saveBodyEdit(editor);
        }
        return error == null ? SaveOutcome.success() : SaveOutcome.failure(error);
    }

    private String saveHeaderEdit(EditorModel editor) {
        try {
            cliClient.saveHeader(editor.path(), editor.header(), editor.project());
            editor.markHeaderSaved();
            return null;
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            return notice.message() + " " + notice.action();
        } catch (RuntimeException exception) {
            return exception.getMessage() == null ? "Error inesperado." : exception.getMessage();
        }
    }

    private String saveBodyEdit(EditorModel editor) {
        try {
            cliClient.saveBody(editor.path(), editor.body(), editor.project());
            editor.markBodySaved();
            return null;
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            return notice.message() + " " + notice.action();
        } catch (RuntimeException exception) {
            return exception.getMessage() == null ? "Error inesperado." : exception.getMessage();
        }
    }

    /** Propaga el cuerpo compartido: simula, muestra el diff y, al confirmar, escribe. */
    private boolean propagate(EditorModel editor) {
        try {
            SaveData dryRun = cliClient.saveGroup(editor.path(), editor.sharedBody(), true, editor.project());
            if (!dryRun.written()) {
                editor.markSharedBodySaved();
                previewPanel.showSaveStatus("Sin cambios que propagar.", false);
                return true;
            }
            boolean confirmed = DiffReviewDialog.confirm(stage, "Propagar cambios",
                "Revisa los cambios antes de propagar al grupo.", DiffReview.summary(dryRun));
            if (!confirmed) {
                previewPanel.showSaveStatus("Propagación cancelada.", false);
                return false;
            }
            SaveData result = cliClient.saveGroup(editor.path(), editor.sharedBody(), false, editor.project());
            editor.markSharedBodySaved();
            previewPanel.showSaveStatus("Propagado a " + result.members().size() + " miembros.", false);
            refreshTree();
            return true;
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
            return false;
        } catch (RuntimeException exception) {
            previewPanel.showSaveStatus(
                "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
            return false;
        }
    }

    /** Vincula un artefacto legacy o desvinculado a su grupo: simula, muestra el diff y, al confirmar, aplica. */
    private void adopt(EditorModel editor) {
        try {
            AdoptData dryRun = cliClient.adopt(editor.path(), editor.project(), false);
            String body = "Grupo: " + dryRun.groupId();
            if (dryRun.diff() != null && !dryRun.diff().isBlank()) {
                body += "\n\n" + dryRun.diff();
            } else {
                body += "\n\nSin cambios de contenido; el artefacto se vinculará tal cual.";
            }
            boolean confirmed = DiffReviewDialog.confirm(stage, "Volver al grupo",
                "Revisa el cambio antes de vincular el artefacto a su grupo.", body);
            if (!confirmed) {
                previewPanel.showSaveStatus("Adopción cancelada.", false);
                return;
            }
            cliClient.adopt(editor.path(), editor.project(), true);
            previewPanel.showSaveStatus("Vinculado al grupo.", false);
            refreshTree();
            reloadSelectedArtifact();
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
        } catch (RuntimeException exception) {
            previewPanel.showSaveStatus(
                "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
        }
    }

    /** Vuelve a describir el artefacto seleccionado para reflejar el nuevo estado. */
    private void reloadSelectedArtifact() {
        TreeItem<TreeData.Node> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null) {
            loadArtifact(selected.getValue());
        }
    }

    /** Guarda el contenido de un fichero específico de una skill. */
    private void saveSkillFile(SkillEditorModel skillEditor, String file) {
        try {
            String content = skillEditor.getContent(file);
            cliClient.saveBody(skillEditor.skillPath(), content, skillEditor.project(), file);
            skillEditor.markSaved(file);
            previewPanel.showSaveStatus("Fichero " + file + " guardado.", false);
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
        } catch (RuntimeException exception) {
            previewPanel.showSaveStatus(
                "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
        }
    }

    /** Propaga el contenido de un fichero de skill a otras instancias del mismo grupo. */
    private void propagateSkillFile(SkillEditorModel skillEditor, String file) {
        try {
            String content = skillEditor.getContent(file);
            
            // Primero hacer dry-run para ver qué cambiaría
            SaveData dryRun = cliClient.saveGroup(skillEditor.skillPath(), content, true, 
                skillEditor.project(), file);
            
            if (!dryRun.written()) {
                previewPanel.showSaveStatus("Sin cambios que propagar.", false);
                return;
            }
            
            // Mostrar diff y pedir confirmación
            boolean confirmed = DiffReviewDialog.confirm(stage, "Propagar cambios",
                "Se actualizarán las siguientes copias de la skill:", DiffReview.summary(dryRun));
            if (!confirmed) {
                previewPanel.showSaveStatus("Propagación cancelada.", false);
                return;
            }
            
            // Ejecutar la propagación real
            SaveData result = cliClient.saveGroup(skillEditor.skillPath(), content, false, 
                skillEditor.project(), file);
            skillEditor.markSaved(file);
            previewPanel.showSaveStatus("Propagado a " + result.members().size() + " copias.", false);
            refreshTree();
            reloadSelectedArtifact();
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
        } catch (RuntimeException exception) {
            previewPanel.showSaveStatus(
                "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
        }
    }

    private void applySaveOutcome(EditorModel editor, SaveOutcome outcome) {
        previewPanel.showSaveStatus(outcome.ok() ? "Guardado" : "Error: " + outcome.message(), !outcome.ok());
    }

    private record SaveOutcome(boolean ok, String message) {
        static SaveOutcome success() {
            return new SaveOutcome(true, null);
        }

        static SaveOutcome failure(String message) {
            return new SaveOutcome(false, message);
        }
    }

    private void chooseProject() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar proyecto");
        if (projectSession.path() != null) {
            File current = projectSession.path().toFile();
            if (current.isDirectory()) {
                chooser.setInitialDirectory(current);
            }
        }
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            loadProject(selected.toPath());
        }
    }

    private void loadProject(Path path) {
        projectPath.setText(path.toString());
        setProjectState("Validando…", "muted");
        projectSummary.setText("");
        selectProjectButton.setDisable(true);
        initProjectButton.setDisable(true);

        Thread.ofVirtual().start(() -> {
            try {
                StatusData status = projectSession.select(path);
                Platform.runLater(() -> {
                    selectProjectButton.setDisable(false);
                    renderProject(status);
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    selectProjectButton.setDisable(false);
                    showProjectError(path, notice);
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    selectProjectButton.setDisable(false);
                    showProjectError(path, new ErrorNotice(
                        "client.unexpected", path.toString(), exception.getMessage(), "Reintentar."));
                });
            }
        });
    }

    private void initProject() {
        if (projectSession.path() == null) {
            return;
        }
        setProjectState("Iniciando…", "muted");
        selectProjectButton.setDisable(true);
        initProjectButton.setDisable(true);

        Thread.ofVirtual().start(() -> {
            try {
                InitData result = projectSession.init();
                Platform.runLater(() -> {
                    selectProjectButton.setDisable(false);
                    renderProject(projectSession.status());
                    setProjectState(result.created() ? "Proyecto iniciado" : "Ya estaba configurado", "ok");
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    selectProjectButton.setDisable(false);
                    showProjectError(projectSession.path(), notice);
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    selectProjectButton.setDisable(false);
                    showProjectError(projectSession.path(), new ErrorNotice(
                        "client.unexpected", projectSession.path().toString(),
                        exception.getMessage(), "Reintentar."));
                });
            }
        });
    }

    private void renderProject(StatusData status) {
        projectPath.setText(projectSession.path().toString());
        boolean configured = projectSession.configured();
        setProjectState(configured ? "Configurado" : "No configurado", configured ? "ok" : "warning");
        projectSummary.setText(formatProjectSummary(status));
        initProjectButton.setDisable(configured);
        createModuleButton.setDisable(!configured);
        addTechnicianButton.setDisable(!configured || !hasModules(status));
        createArtifactButton.setDisable(providerIds().isEmpty());
        refreshTree();
    }

    private boolean hasModules(StatusData status) {
        return status != null
            && status.project() != null
            && status.project().modules() != null
            && !status.project().modules().isEmpty();
    }

    /** Crea un módulo: pide datos, simula con dry-run, muestra preview y confirma. */
    private void createModule() {
        if (!requireProject()) return;

        var result = CreateModuleDialog.show(stage, providerIds());
        if (result.isEmpty()) return;

        CreateModuleDialog.Result params = result.get();
        if (params.name().isBlank() || params.technicians().isEmpty() || params.providers().isEmpty()) {
            showError("Faltan datos", "Completa nombre, técnicos y proveedores.");
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                CreateModuleData dryRun = cliClient.createModule(
                    projectSession.path(), params.name(), params.technicians(), params.providers(), true);

                Platform.runLater(() -> {
                    boolean confirmed = CreationPreviewDialog.confirm(
                        stage, "Crear módulo", "Se registrará el módulo y se generarán sus artefactos.", dryRun);
                    if (!confirmed) return;

                    Thread.ofVirtual().start(() -> executeCreateModule(
                        params.name(), params.technicians(), params.providers()));
                });
            } catch (CliClientException ex) {
                Platform.runLater(() -> showError("Error", ErrorPresenter.present(ex).message()));
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Error", ex.getMessage()));
            }
        });
    }

    private void executeCreateModule(String name, List<String> technicians, List<String> providers) {
        try {
            CreateModuleData result = cliClient.createModule(
                projectSession.path(), name, technicians, providers, false);

            Platform.runLater(() -> {
                showInfo("Módulo creado",
                    "Se crearon " + result.created().size() + " artefactos.\n" +
                    "Se omitieron " + result.skipped().size() + ".");
                refreshAll();
            });
        } catch (CliClientException ex) {
            Platform.runLater(() -> showError("Error", ErrorPresenter.present(ex).message()));
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Error", ex.getMessage()));
        }
    }

    /** Añade técnicos a un módulo existente: pide módulo, técnicos y proveedores. */
    private void addTechnician() {
        if (!requireProject()) return;
        if (projectSession.status().project().modules().isEmpty()) {
            showError("Sin módulos", "No hay módulos registrados en el proyecto.");
            return;
        }

        String selectedModule = projectSession.status().project().modules().getFirst();
        var result = AddTechnicianDialog.show(stage, selectedModule, providerIds());
        if (result.isEmpty()) return;

        AddTechnicianDialog.Result params = result.get();
        if (params.technicians().isEmpty() || params.providers().isEmpty()) {
            showError("Faltan datos", "Selecciona técnicos y proveedores.");
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                CreateModuleData dryRun = cliClient.addTechnician(
                    projectSession.path(), selectedModule, params.technicians(), params.providers(), true);

                Platform.runLater(() -> {
                    boolean confirmed = CreationPreviewDialog.confirm(
                        stage, "Añadir técnicos",
                        "Se generarán los artefactos para los técnicos en «" + selectedModule + "».", dryRun);
                    if (!confirmed) return;

                    Thread.ofVirtual().start(() -> executeAddTechnician(
                        selectedModule, params.technicians(), params.providers()));
                });
            } catch (CliClientException ex) {
                Platform.runLater(() -> showError("Error", ErrorPresenter.present(ex).message()));
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Error", ex.getMessage()));
            }
        });
    }

    private void executeAddTechnician(String module, List<String> technicians, List<String> providers) {
        try {
            CreateModuleData result = cliClient.addTechnician(
                projectSession.path(), module, technicians, providers, false);

            Platform.runLater(() -> {
                showInfo("Técnicos añadidos",
                    "Se crearon " + result.created().size() + " artefactos.\n" +
                    "Se omitieron " + result.skipped().size() + ".");
                refreshAll();
            });
        } catch (CliClientException ex) {
            Platform.runLater(() -> showError("Error", ErrorPresenter.present(ex).message()));
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Error", ex.getMessage()));
        }
    }

    private void createArtifact() {
        // Verificar que hay un proyecto configurado o permitir global
        boolean hasProject = projectSession != null && projectSession.configured();
        List<String> providers = providerIds();
        if (providers.isEmpty()) {
            showError("Sin proveedores", "No hay proveedores configurados.");
            return;
        }

        // Paso 1: Selección inicial
        Optional<CreateArtifactDialog.Result> selectionOpt = CreateArtifactDialog.show(stage, providers, hasProject);
        if (selectionOpt.isEmpty()) return;
        CreateArtifactDialog.Result selection = selectionOpt.get();

        // Validar que si es project, hay proyecto configurado
        if ("project".equals(selection.scope()) && !hasProject) {
            showError("Proyecto requerido", "No hay proyecto configurado. Selecciona un proyecto o usa ámbito global.");
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                // Paso 2: Obtener lista de nombres disponibles
                CatalogData catalogList = cliClient.catalog(
                    selection.provider(), selection.scope(), selection.kind(), null,
                    "project".equals(selection.scope()) ? projectSession.path() : null);

                if (catalogList.entries().isEmpty()) {
                    Platform.runLater(() -> showError("Sin artefactos",
                        "No hay artefactos disponibles en el catálogo para esta combinación."));
                    return;
                }

                // Paso 3: Seleccionar nombre del catálogo
                Platform.runLater(() -> {
                    Optional<String> nameOpt = selectNameFromCatalog(catalogList);
                    if (nameOpt.isEmpty()) return;
                    String name = nameOpt.get();

                    // Paso 4: Obtener plantilla con --name
                    Thread.ofVirtual().start(() -> {
                        try {
                            CatalogData catalogWithTemplate = cliClient.catalog(
                                selection.provider(), selection.scope(), selection.kind(), name,
                                "project".equals(selection.scope()) ? projectSession.path() : null);

                            if (catalogWithTemplate.entries().isEmpty()) {
                                Platform.runLater(() -> showError("Error",
                                    "No se encontró la plantilla para «" + name + "»."));
                                return;
                            }

                            CatalogData.CatalogEntry entry = catalogWithTemplate.entries().getFirst();

                            // Paso 5: Editar plantilla
                            Platform.runLater(() -> {
                                Optional<EditTemplateDialog.Result> editOpt = EditTemplateDialog.show(
                                    stage, name, entry.template(), entry.exists());
                                if (editOpt.isEmpty()) return;
                                EditTemplateDialog.Result edit = editOpt.get();

                                // Paso 6: Crear artefacto
                                Thread.ofVirtual().start(() -> executeCreateArtifact(
                                    selection, name, edit, entry));
                            });
                        } catch (CliClientException ex) {
                            Platform.runLater(() -> showError("Error", ErrorPresenter.present(ex).message()));
                        } catch (Exception ex) {
                            Platform.runLater(() -> showError("Error", ex.getMessage()));
                        }
                    });
                });
            } catch (CliClientException ex) {
                Platform.runLater(() -> showError("Error", ErrorPresenter.present(ex).message()));
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Error", ex.getMessage()));
            }
        });
    }

    private Optional<String> selectNameFromCatalog(CatalogData catalogList) {
        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(
            catalogList.entries().getFirst().name(),
            catalogList.entries().stream().map(CatalogData.CatalogEntry::name).toList());
        dialog.initOwner(stage);
        dialog.setTitle("Seleccionar artefacto");
        dialog.setHeaderText("Elige el artefacto que deseas crear:");
        return dialog.showAndWait();
    }

    private void executeCreateArtifact(CreateArtifactDialog.Result selection, String name,
                                       EditTemplateDialog.Result edit, CatalogData.CatalogEntry entry) {
        try {
            Path project = "project".equals(selection.scope()) ? projectSession.path() : null;
            CreateArtifactData result = cliClient.createArtifact(
                selection.provider(), selection.scope(), selection.kind(), name,
                project, edit.content(), edit.overwrite());

            Platform.runLater(() -> {
                if (result.replaced()) {
                    showInfo("Artefacto creado",
                        "El artefacto «" + name + "» se creó sobrescribiendo el existente.\n" +
                        "Ruta: " + result.path());
                } else {
                    showInfo("Artefacto creado",
                        "El artefacto «" + name + "» se creó correctamente.\n" +
                        "Ruta: " + result.path());
                }
                refreshAll();
            });
        } catch (CliClientException ex) {
            Platform.runLater(() -> showError("Error", ErrorPresenter.present(ex).message()));
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Error", ex.getMessage()));
        }
    }

    private boolean requireProject() {
        if (projectSession.path() == null) {
            showError("Sin proyecto", "Selecciona un proyecto primero.");
            return false;
        }
        if (!projectSession.configured()) {
            showError("Proyecto no configurado", "Inicia el proyecto antes de crear módulos.");
            return false;
        }
        return true;
    }

    private List<String> providerIds() {
        if (projectSession.status() == null || projectSession.status().providers() == null) {
            return List.of();
        }
        return projectSession.status().providers().stream()
            .map(StatusData.Provider::id)
            .toList();
    }

    private void refreshAll() {
        try {
            StatusData status = projectSession.select(projectSession.path());
            Platform.runLater(() -> renderProject(status));
        } catch (CliClientException ex) {
            Platform.runLater(() -> showError("Error al refrescar", ErrorPresenter.present(ex).message()));
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Error al refrescar", ex.getMessage()));
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showProjectError(Path path, ErrorNotice notice) {
        projectPath.setText(path == null ? "" : path.toString());
        setProjectState("Error " + notice.code(), "error");
        projectSummary.setText("Mensaje: " + notice.message()
            + "\nAcción: " + notice.action());
        initProjectButton.setDisable(true);
    }

    private void setProjectState(String text, String style) {
        projectState.setText(text);
        projectState.getStyleClass().removeAll("muted", "ok", "error", "warning");
        projectState.getStyleClass().add(style);
    }

    private Path currentProject() {
        return projectSession == null ? null : projectSession.path();
    }

    private void runRaw(List<String> args, TextArea output, Label state) {
        state.setText("Ejecutando…");
        output.clear();

        Thread.ofVirtual().start(() -> {
            try {
                CliResult result = new CliProcess(cliCommand).run(args);
                String text = formatResult(result);
                Platform.runLater(() -> {
                    output.setText(text);
                    state.setText(result.timedOut() ? "Timeout" : "Código " + result.exitCode());
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    output.setText(exception.getMessage());
                    state.setText("Error");
                });
            }
        });
    }

    private void runTyped(String label, Callable<String> action, TextArea output, Label state) {
        state.setText("Ejecutando…");
        output.clear();

        Thread.ofVirtual().start(() -> {
            try {
                String text = action.call();
                Platform.runLater(() -> {
                    output.setText(text);
                    state.setText(label + " OK");
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    output.setText(formatNotice(notice));
                    state.setText("Error " + notice.code());
                });
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    output.setText(exception.getMessage());
                    state.setText("Error");
                });
            }
        });
    }

    private static String formatProjectSummary(StatusData status) {
        StringBuilder text = new StringBuilder();
        if (status.providers() != null && !status.providers().isEmpty()) {
            text.append("Proveedores: ")
                .append(status.providers().stream()
                    .map(provider -> provider.name() + "=" + provider.state())
                    .collect(Collectors.joining(" · ")));
        }
        if (status.project() != null) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append("Proyecto: ")
                .append(status.project().initialized() ? "con .sdd" : "sin .sdd");
            int modules = status.project().modules() == null ? 0 : status.project().modules().size();
            text.append(" · módulos: ").append(modules);
            if (status.project().warnings() != null && !status.project().warnings().isEmpty()) {
                text.append(" · avisos: ").append(status.project().warnings().size());
            }
        }
        if (text.isEmpty()) {
            text.append("Sin datos del proyecto.");
        }
        return text.toString();
    }

    private static String formatTreeWarnings(TreeData data) {
        StringBuilder text = new StringBuilder();
        if (data.warnings() != null) {
            for (TreeData.Warning warning : data.warnings()) {
                text.append("Aviso ").append(warning.type())
                    .append(" (línea ").append(warning.line()).append(")\n");
            }
        }
        if (data.problems() != null) {
            for (TreeData.Problem problem : data.problems()) {
                text.append("Problema ").append(problem.code())
                    .append(": ").append(problem.path()).append('\n');
            }
        }
        return text.toString().trim();
    }

    private static String formatNode(TreeData.Node node) {
        StringBuilder text = new StringBuilder();
        text.append("Tipo: ").append(node.type()).append('\n');
        text.append("Nombre: ").append(node.name()).append('\n');
        if (node.provider() != null) {
            text.append("Proveedor: ").append(node.provider()).append('\n');
        }
        if (node.status() != null) {
            text.append("Estado: ").append(node.status()).append('\n');
        }
        if (node.pending()) {
            text.append("Pendiente: sí\n");
        }
        if (node.path() != null) {
            text.append("Ruta: ").append(node.path()).append('\n');
        }
        return text.toString().trim();
    }

    private static String formatResult(CliResult result) {
        StringBuilder text = new StringBuilder();
        text.append("exit: ").append(result.exitCode())
            .append(" · ").append(result.durationMillis()).append(" ms");
        if (result.timedOut()) {
            text.append(" · TIMEOUT");
        }
        text.append("\n\n=== stdout ===\n").append(result.stdout());
        if (!result.stderr().isBlank()) {
            text.append("\n=== stderr ===\n").append(result.stderr());
        }
        return text.toString();
    }

    private static String formatStatus(StatusData data) {
        StringBuilder text = new StringBuilder("Proveedores:\n");
        if (data.providers() != null) {
            for (StatusData.Provider provider : data.providers()) {
                text.append("  ").append(provider.name())
                    .append(" (").append(provider.id()).append("): ")
                    .append(provider.state());
                if (provider.missing() != null && !provider.missing().isEmpty()) {
                    text.append(" · faltan ").append(provider.missing().size());
                }
                text.append('\n');
            }
        }

        if (data.project() == null) {
            text.append("Proyecto: (sin proyecto)");
        } else {
            text.append("Proyecto: ").append(data.project().path()).append('\n');
            text.append("  inicializado: ").append(data.project().initialized()).append('\n');
            int modules = data.project().modules() == null ? 0 : data.project().modules().size();
            text.append("  módulos: ").append(modules);
            if (data.project().warnings() != null && !data.project().warnings().isEmpty()) {
                text.append(" · avisos: ").append(data.project().warnings().size());
            }
        }
        return text.toString();
    }

    private static String formatNotice(ErrorNotice notice) {
        return "ERROR " + notice.code()
            + "\nRuta: " + (notice.path() == null ? "-" : notice.path())
            + "\nMensaje: " + notice.message()
            + "\nAcción: " + notice.action();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
