package es.vargontoc.agentic.viewer;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
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
import es.vargontoc.agentic.viewer.cli.model.DescribeData;
import es.vargontoc.agentic.viewer.cli.model.InitData;
import es.vargontoc.agentic.viewer.cli.model.StatusData;
import es.vargontoc.agentic.viewer.cli.model.TreeData;
import es.vargontoc.agentic.viewer.tree.TreeLabels;
import es.vargontoc.agentic.viewer.tree.TreeModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

    private TreeView<TreeData.Node> treeView;
    private TreeItem<TreeData.Node> treeRoot;
    private Label treeWarnings;
    private Label selectionInfo;

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

        Label subtitle = new Label("JavaFX · TASK-005: árbol global y de proyecto");
        subtitle.getStyleClass().add("subtitle");

        VBox header = new VBox(4, title, subtitle);

        VBox content = new VBox(16, projectCard(), cliCard());
        ScrollPane main = new ScrollPane(content);
        main.setFitToWidth(true);
        main.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        main.getStyleClass().add("main-scroll");

        SplitPane split = new SplitPane(sidebar(), main);
        split.setDividerPositions(0.34);

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
            if (selected == null || selected.getValue() == null) {
                selectionInfo.setText("");
            } else {
                selectionInfo.setText(formatNode(selected.getValue()));
            }
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

        boolean missing = projectSession == null;
        selectProjectButton.setDisable(missing);
        if (missing) {
            setProjectState(cliError == null ? "CLI no disponible" : cliError, "error");
        }

        HBox actions = new HBox(8, selectProjectButton, initProjectButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, heading, projectPath, projectState, projectSummary, actions);
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

        TextField artifactPath = new TextField();
        artifactPath.setPromptText("Ruta de un agente o skill para describe");
        Button describe = new Button("describe");
        describe.setOnAction(event -> runTyped("describe",
            () -> formatDescribe(cliClient.describe(pathFrom(artifactPath), currentProject())), output, state));
        HBox describeRow = new HBox(8, artifactPath, describe);
        describeRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(artifactPath, Priority.ALWAYS);

        boolean missing = cliClient == null;
        rawVersion.setDisable(missing);
        status.setDisable(missing);
        describe.setDisable(missing);
        artifactPath.setDisable(missing);

        VBox card = new VBox(10, heading, path, primary, describeRow, state, output);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        return card;
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
        refreshTree();
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

    private static Path pathFrom(TextField field) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Escribe la ruta de un agente o skill.");
        }
        return Path.of(value);
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

    private static String formatDescribe(DescribeData data) {
        StringBuilder text = new StringBuilder();
        text.append("provider: ").append(data.provider()).append('\n');
        text.append("scope: ").append(data.scope()).append('\n');
        text.append("kind: ").append(data.kind()).append('\n');
        text.append("name: ").append(data.name()).append('\n');
        text.append("format: ").append(data.format() == null ? "-" : data.format()).append('\n');
        text.append("module: ").append(data.module() == null ? "-" : data.module()).append('\n');
        text.append("status: ").append(data.status()).append('\n');
        text.append("modifiedExternally: ").append(data.modifiedExternally()).append('\n');
        text.append("hash: ").append(data.hash()).append('\n');
        text.append("header: ").append(lineCount(data.header())).append(" líneas\n");
        text.append("body: ").append(lineCount(data.body())).append(" líneas\n");
        text.append("sharedBody: ")
            .append(data.sharedBody() == null ? "-" : lineCount(data.sharedBody()) + " líneas")
            .append('\n');
        text.append("files: ").append(data.files() == null ? 0 : data.files().size());
        return text.toString();
    }

    private static int lineCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) text.lines().count();
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
