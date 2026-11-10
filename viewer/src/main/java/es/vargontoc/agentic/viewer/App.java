package es.vargontoc.agentic.viewer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.agentic.viewer.cli.CliClient;
import es.vargontoc.agentic.viewer.cli.CliClientException;
import es.vargontoc.agentic.viewer.cli.CliCommand;
import es.vargontoc.agentic.viewer.cli.CliLocator;
import es.vargontoc.agentic.viewer.cli.CliProcess;
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
import es.vargontoc.agentic.viewer.tree.TreeFilter;
import es.vargontoc.agentic.viewer.tree.TreeLabels;
import es.vargontoc.agentic.viewer.tree.TreeModel;
import es.vargontoc.agentic.viewer.ui.Icons;
import es.vargontoc.agentic.viewer.ui.Theme;
import es.vargontoc.agentic.viewer.ui.WelcomePane;
import es.vargontoc.agentic.viewer.watcher.CacheStore;
import es.vargontoc.agentic.viewer.watcher.ProjectWatcher;
import es.vargontoc.agentic.viewer.watcher.ProjectWatcher.ChangeEvent;
import es.vargontoc.agentic.viewer.watcher.StaleTracker;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.OverrunStyle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * Viewer JavaFX: navegación por el árbol de artefactos, previsualización y
 * edición, consola PTY integrada y operaciones del CLI.
 */
public final class App extends Application {

    private static final String[] TREE_CLASSES = {
        "tree-root", "tree-provider", "tree-agent", "tree-skill", "tree-profile",
        "tree-sdd", "tree-module", "tree-technician", "tree-pending", "tree-legacy",
        "tree-detached", "tree-stale", "tree-dirty"
    };

    private Stage stage;
    private CliCommand cliCommand;
    private CliClient cliClient;
    private ProjectSession projectSession;
    private StatusData globalStatus;
    private String cliError;

    private ProjectWatcher projectWatcher;
    private CacheStore cacheStore;
    private StaleTracker staleTracker;

    private Label projectPathLabel;
    private Label projectStateChip;
    private Label projectSummaryLabel;
    private Button openProjectButton;
    private Button initProjectButton;
    private MenuButton createMenuButton;

    private MenuItem openProjectMenuItem;
    private MenuItem reloadMenuItem;
    private MenuItem saveMenuItem;
    private MenuItem closeProjectMenuItem;
    private MenuItem initProjectMenuItem;
    private final List<MenuItem> createModuleItems = new ArrayList<>();
    private final List<MenuItem> addTechnicianItems = new ArrayList<>();
    private final List<MenuItem> createArtifactItems = new ArrayList<>();
    private MenuItem searchMenuItem;
    private MenuItem consoleMenuItem;
    private CheckMenuItem autoRefreshMenuItem;

    private TreeView<TreeData.Node> treeView;
    private TreeItem<TreeData.Node> treeRoot;
    private TreeData.Node selectedNode;
    private TreeData lastTreeData;
    private TextField searchField;
    private Label filterStatus;
    private Label treeWarnings;
    private Label staleWarning;
    private HBox staleBanner;
    private Label selectionInfo;
    private Button sidebarRefreshButton;
    private boolean suppressSelection;

    private PreviewPanel previewPanel;
    private WelcomePane welcomePane;
    private StackPane centerHost;
    private TabPane workspaceTabs;
    private Tab consoleTab;
    private PtyConsolePanel ptyConsolePanel;

    private Label statusMessage;
    private Label dirtyStatus;
    private Label watcherStatus;
    private ProgressIndicator busySpinner;
    private CheckBox autoRefreshCheck;
    private PauseTransition autoRefreshDelay;
    private int busyCount;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        initServices();

        previewPanel = new PreviewPanel();
        previewPanel.setOnSddFileSelected(this::loadSddFile);
        previewPanel.setOnSave(this::saveEditorAsync);
        previewPanel.setOnPropagate(this::propagate);
        previewPanel.setOnAdopt(this::adopt);
        previewPanel.setOnSaveSkillFile(this::saveSkillFile);
        previewPanel.setOnSaveAllSkillFiles(this::saveAllSkillFiles);
        previewPanel.setOnPropagateSkillFile(this::propagateSkillFile);
        previewPanel.setOnDirtyChanged(this::onEditorDirtyChanged);

        ptyConsolePanel = new PtyConsolePanel(cliCommand, this::currentProject);
        ptyConsolePanel.setOnStateChanged(this::onConsoleStateChanged);

        welcomePane = new WelcomePane();
        welcomePane.setOnOpenProject(this::chooseProject);
        welcomePane.setOnInitProject(this::initProject);
        welcomePane.setOnCreateArtifact(this::createArtifact);

        workspaceTabs = new TabPane();
        workspaceTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        workspaceTabs.getStyleClass().add("workspace-tabs");
        Tab artifactTab = new Tab("Artefacto", previewPanel);
        artifactTab.setClosable(false);
        artifactTab.setGraphic(Icons.of(Icons.Icon.FILE_TEXT, 14, "tab-icon"));
        consoleTab = new Tab("Consola", consoleContent());
        consoleTab.setClosable(false);
        consoleTab.setGraphic(Icons.of(Icons.Icon.TERMINAL, 14, "tab-icon"));
        workspaceTabs.getTabs().addAll(artifactTab, consoleTab);

        centerHost = new StackPane(welcomePane, workspaceTabs);
        centerHost.getStyleClass().add("workspace");
        centerHost.setPadding(new Insets(14));

        autoRefreshDelay = new PauseTransition(Duration.millis(650));
        autoRefreshDelay.setOnFinished(event -> autoRefresh());

        VBox top = new VBox(menuBar(), headerBar());
        SplitPane body = new SplitPane(sidebar(), centerHost);
        body.getStyleClass().add("main-split");
        body.setDividerPositions(0.27);
        SplitPane.setResizableWithParent(body.getItems().getFirst(), false);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app");
        root.setTop(top);
        root.setCenter(body);
        root.setBottom(statusBar());

        Scene scene = new Scene(root, 1320, 880);
        Theme.apply(scene);

        stage.setTitle("Agentic Viewer");
        stage.setMinWidth(1040);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (!confirmCloseApplication()) {
                event.consume();
                return;
            }
            closeResources();
        });

        stage.show();

        updateWelcomeState();
        updateMenuStates();
        updateWatcherStatus();
        updateWindowTitle();
        showWelcome();

        if (cliError != null) {
            welcomePane.setCli(false, cliError);
            showErrorAlert("Error de aplicación", "No se encontró el CLI", cliError);
        } else {
            welcomePane.setCli(true, null);
            refreshTree();
            loadGlobalStatus();
        }
    }

    private void initServices() {
        try {
            cliCommand = CliLocator.resolve(Path.of("").toAbsolutePath());
            cliClient = new CliClient(new CliProcess(cliCommand), new ObjectMapper());
            projectSession = new ProjectSession(cliClient);
            cacheStore = new CacheStore();
            staleTracker = new StaleTracker();
            projectWatcher = new ProjectWatcher(this::onFileSystemChange);
        } catch (RuntimeException exception) {
            cliError = exception.getMessage();
        }
    }

    private Node consoleContent() {
        VBox box = new VBox(ptyConsolePanel);
        VBox.setVgrow(ptyConsolePanel, Priority.ALWAYS);
        return box;
    }

    // ------------------------------------------------------------------
    // Estructura de la ventana
    // ------------------------------------------------------------------

    private MenuBar menuBar() {
        Menu file = new Menu("Archivo");
        openProjectMenuItem = menuItem("Abrir proyecto…", "Shortcut+O", Icons.Icon.FOLDER_OPEN, this::chooseProject);
        reloadMenuItem = menuItem("Recargar", "F5", Icons.Icon.REFRESH, this::reloadWithGuard);
        saveMenuItem = menuItem("Guardar", "Shortcut+S", Icons.Icon.SAVE, this::saveCurrent);
        closeProjectMenuItem = menuItem("Cerrar proyecto", null, Icons.Icon.CLOSE, this::closeProject);
        MenuItem exit = menuItem("Salir", "Shortcut+Q", Icons.Icon.LOG_OUT, this::requestExit);
        file.getItems().addAll(openProjectMenuItem, reloadMenuItem, saveMenuItem,
            new SeparatorMenuItem(), closeProjectMenuItem, new SeparatorMenuItem(), exit);

        Menu project = new Menu("Proyecto");
        initProjectMenuItem = menuItem("Iniciar proyecto", null, Icons.Icon.PLAY, this::initProject);
        MenuItem createModule = menuItem("Crear módulo…", "Shortcut+M", Icons.Icon.MODULE, this::createModule);
        MenuItem addTechnician = menuItem("Añadir técnico…", "Shortcut+Shift+T", Icons.Icon.TECHNICIAN, this::addTechnician);
        MenuItem createArtifact = menuItem("Crear artefacto…", "Shortcut+N", Icons.Icon.PLUS, this::createArtifact);
        createModuleItems.add(createModule);
        addTechnicianItems.add(addTechnician);
        createArtifactItems.add(createArtifact);
        project.getItems().addAll(initProjectMenuItem, createModule,
            addTechnician, createArtifact);

        Menu view = new Menu("Ver");
        searchMenuItem = menuItem("Buscar artefactos", "Shortcut+F", Icons.Icon.SEARCH, this::focusSearch);
        consoleMenuItem = menuItem("Consola", "F6", Icons.Icon.TERMINAL, this::showConsole);
        autoRefreshMenuItem = new CheckMenuItem("Actualización automática");
        autoRefreshMenuItem.setSelected(true);
        view.getItems().addAll(searchMenuItem, consoleMenuItem,
            new SeparatorMenuItem(), autoRefreshMenuItem);

        Menu help = new Menu("Ayuda");
        help.getItems().addAll(menuItem("Atajos de teclado", null, Icons.Icon.HELP, this::showShortcuts),
            menuItem("Acerca de", null, Icons.Icon.INFO, this::showAbout));

        MenuBar bar = new MenuBar(file, project, view, help);
        bar.getStyleClass().add("app-menu");
        return bar;
    }

    private MenuItem menuItem(String text, String accelerator, Runnable action) {
        MenuItem item = new MenuItem(text);
        if (accelerator != null) {
            item.setAccelerator(KeyCombination.keyCombination(accelerator));
        }
        item.setOnAction(event -> action.run());
        return item;
    }

    private MenuItem menuItem(String text, String accelerator, Icons.Icon icon, Runnable action) {
        MenuItem item = menuItem(text, accelerator, action);
        if (icon != null) {
            item.setGraphic(Icons.of(icon, 14, "menu-icon"));
        }
        return item;
    }

    private Node headerBar() {
        Node mark = Icons.of(Icons.Icon.AGENT, 30, "brand-icon");

        Label title = new Label("Agentic Viewer");
        title.getStyleClass().add("brand-title");

        Label tagline = new Label("Agentes y skills de tu proyecto, en un solo lugar.");
        tagline.getStyleClass().add("brand-tagline");

        VBox brandText = new VBox(1, title, tagline);
        HBox brand = new HBox(10, mark, brandText);
        brand.setAlignment(Pos.CENTER_LEFT);

        openProjectButton = new Button("Abrir proyecto");
        openProjectButton.getStyleClass().add("secondary");
        openProjectButton.setGraphic(Icons.of(Icons.Icon.FOLDER_OPEN, 15, "button-icon"));
        openProjectButton.setTooltip(new Tooltip("Abrir un proyecto (Ctrl+O)"));
        openProjectButton.setOnAction(event -> chooseProject());

        initProjectButton = new Button("Iniciar proyecto");
        initProjectButton.getStyleClass().add("primary");
        initProjectButton.setGraphic(Icons.of(Icons.Icon.PLAY, 15, "button-icon"));
        initProjectButton.setTooltip(new Tooltip("Crear la estructura .sdd del proyecto"));
        initProjectButton.setOnAction(event -> initProject());
        initProjectButton.setManaged(false);
        initProjectButton.setVisible(false);

        createMenuButton = new MenuButton("Crear");
        createMenuButton.getStyleClass().addAll("primary", "create-menu");
        createMenuButton.setGraphic(Icons.of(Icons.Icon.PLUS, 15, "button-icon"));
        MenuItem quickModule = new MenuItem("Módulo…");
        quickModule.setGraphic(Icons.of(Icons.Icon.MODULE, 14, "menu-icon"));
        quickModule.setOnAction(event -> createModule());
        MenuItem quickTechnician = new MenuItem("Añadir técnico…");
        quickTechnician.setGraphic(Icons.of(Icons.Icon.TECHNICIAN, 14, "menu-icon"));
        quickTechnician.setOnAction(event -> addTechnician());
        MenuItem quickArtifact = new MenuItem("Artefacto…");
        quickArtifact.setGraphic(Icons.of(Icons.Icon.FILE_PLUS, 14, "menu-icon"));
        quickArtifact.setOnAction(event -> createArtifact());
        createModuleItems.add(quickModule);
        addTechnicianItems.add(quickTechnician);
        createArtifactItems.add(quickArtifact);
        createMenuButton.getItems().addAll(quickModule, quickTechnician,
            new SeparatorMenuItem(), quickArtifact);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(12, brand, spacer, openProjectButton, initProjectButton, createMenuButton);
        topRow.setAlignment(Pos.CENTER_LEFT);

        projectStateChip = new Label("Sin proyecto");
        projectStateChip.getStyleClass().addAll("chip", "muted");

        projectPathLabel = new Label("Ningún proyecto seleccionado");
        projectPathLabel.getStyleClass().add("project-path");
        projectPathLabel.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        projectPathLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(projectPathLabel, Priority.ALWAYS);

        projectSummaryLabel = new Label();
        projectSummaryLabel.getStyleClass().add("project-summary");
        projectSummaryLabel.setWrapText(true);
        projectSummaryLabel.setManaged(false);
        projectSummaryLabel.setVisible(false);

        HBox projectRow = new HBox(10, projectStateChip, projectPathLabel);
        projectRow.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(8, topRow, projectRow, projectSummaryLabel);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(12, 18, 12, 18));
        return header;
    }

    private Node sidebar() {
        Label heading = new Label("Navegación");
        heading.getStyleClass().add("heading");

        sidebarRefreshButton = new Button("", Icons.of(Icons.Icon.REFRESH, 15, "button-icon"));
        sidebarRefreshButton.getStyleClass().addAll("ghost", "icon");
        sidebarRefreshButton.setTooltip(new Tooltip("Recargar (F5)"));
        sidebarRefreshButton.setAccessibleText("Recargar el árbol de artefactos");
        sidebarRefreshButton.setOnAction(event -> reloadWithGuard());

        Region headingSpacer = new Region();
        HBox.setHgrow(headingSpacer, Priority.ALWAYS);
        HBox headingRow = new HBox(8, heading, headingSpacer, sidebarRefreshButton);
        headingRow.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Buscar artefactos…  (Ctrl+F)");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((observable, previous, current) -> applyTreeFilter());
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                searchField.clear();
                treeView.requestFocus();
            }
        });

        HBox searchRow = new HBox(8, Icons.of(Icons.Icon.SEARCH, 15, "search-icon"), searchField);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getStyleClass().add("search-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        filterStatus = new Label();
        filterStatus.getStyleClass().add("filter-status");
        filterStatus.setWrapText(true);
        filterStatus.setManaged(false);
        filterStatus.setVisible(false);

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
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                boolean dirty = previewPanel != null && previewPanel.isDirtyNode(node.path());
                setText(TreeLabels.label(node) + (dirty ? "  ●" : ""));
                setGraphic(treeIcon(node));
                getStyleClass().add("tree-" + node.type());
                if (dirty) {
                    getStyleClass().add("tree-dirty");
                }
                if (node.pending()) {
                    getStyleClass().add("tree-pending");
                }
                if ("legacy".equals(node.status())) {
                    getStyleClass().add("tree-legacy");
                }
                if ("detached".equals(node.status())) {
                    getStyleClass().add("tree-detached");
                }
                String tooltip = node.path();
                if (dirty) {
                    tooltip = "Cambios sin guardar" + (tooltip == null ? "" : "\n" + tooltip);
                }
                if (staleTracker != null && node.path() != null && staleTracker.isStale(node.path())) {
                    getStyleClass().add("tree-stale");
                    tooltip = (tooltip == null ? "" : tooltip + "\n") + "Desactualizado: hay cambios externos";
                }
                setTooltip(tooltip == null ? null : new Tooltip(tooltip));
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
            selectedNode = node;
            if (node == null) {
                hideSelectionInfo();
            } else {
                updateSelectionInfo(node);
            }
            onSelectNode(node);
        });

        treeWarnings = new Label(cliError == null ? "" : cliError);
        treeWarnings.getStyleClass().add(cliError == null ? "muted" : "error");
        treeWarnings.setWrapText(true);
        treeWarnings.setManaged(cliError != null);
        treeWarnings.setVisible(cliError != null);

        staleWarning = new Label();
        staleWarning.getStyleClass().add("warning");
        staleWarning.setWrapText(true);

        Button staleReload = new Button("Recargar ahora");
        staleReload.getStyleClass().addAll("secondary", "small");
        staleReload.setGraphic(Icons.of(Icons.Icon.REFRESH, 13, "button-icon"));
        staleReload.setOnAction(event -> reloadWithGuard());

        staleBanner = new HBox(8, staleWarning, staleReload);
        staleBanner.setAlignment(Pos.CENTER_LEFT);
        staleBanner.getStyleClass().add("stale-banner");
        staleBanner.setManaged(false);
        staleBanner.setVisible(false);

        selectionInfo = new Label();
        selectionInfo.getStyleClass().add("selection");
        selectionInfo.setWrapText(true);
        selectionInfo.setManaged(false);
        selectionInfo.setVisible(false);

        VBox sidebar = new VBox(10, headingRow, searchRow, filterStatus, treeView,
            treeWarnings, staleBanner, selectionInfo);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(14));
        sidebar.setPrefWidth(350);
        sidebar.setMinWidth(280);
        return sidebar;
    }

    private HBox statusBar() {
        busySpinner = new ProgressIndicator();
        busySpinner.setPrefSize(14, 14);
        busySpinner.setMinSize(14, 14);
        busySpinner.setMaxSize(14, 14);
        busySpinner.setManaged(false);
        busySpinner.setVisible(false);

        statusMessage = new Label("Listo");
        statusMessage.getStyleClass().add("status-message");

        dirtyStatus = new Label("Cambios sin guardar");
        dirtyStatus.getStyleClass().add("warning");
        dirtyStatus.setGraphic(Icons.of(Icons.Icon.PENCIL, 12, "status-icon"));
        dirtyStatus.setManaged(false);
        dirtyStatus.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        autoRefreshCheck = new CheckBox("Auto-actualizar");
        autoRefreshCheck.setTooltip(new Tooltip("Refresca el árbol cuando cambian ficheros externos"));
        if (autoRefreshMenuItem != null) {
            autoRefreshCheck.selectedProperty().bindBidirectional(autoRefreshMenuItem.selectedProperty());
        }

        watcherStatus = new Label("Watcher inactivo");
        watcherStatus.getStyleClass().add("status-meta");
        watcherStatus.setGraphic(Icons.of(Icons.Icon.REFRESH, 12, "status-icon"));

        HBox bar = new HBox(10, busySpinner, statusMessage, dirtyStatus, spacer,
            autoRefreshCheck, watcherStatus);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");
        bar.setPadding(new Insets(6, 14, 6, 14));
        return bar;
    }

    // ------------------------------------------------------------------
    // Estado y navegación
    // ------------------------------------------------------------------

    private void refreshTree() {
        if (cliClient == null) {
            return;
        }
        treeWarnings.setText("Cargando árbol…");
        treeWarnings.getStyleClass().removeAll("muted", "warning", "error");
        treeWarnings.getStyleClass().add("muted");
        treeWarnings.setManaged(true);
        treeWarnings.setVisible(true);
        beginBusy("Cargando árbol…");

        Thread.ofVirtual().start(() -> {
            try {
                TreeData data = cliClient.tree(currentProject());
                Platform.runLater(() -> {
                    updateTree(data);
                    endBusy("Árbol actualizado");
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    showTreeError(notice);
                    endBusy("Error al cargar el árbol");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    showTreeError(new ErrorNotice(
                        "client.unexpected", null, exception.getMessage(), "Reintentar."));
                    endBusy("Error al cargar el árbol");
                });
            }
        });
    }

    private void updateTree(TreeData data) {
        lastTreeData = data;
        renderTree();
        String warnings = formatTreeWarnings(data);
        treeWarnings.setText(warnings);
        treeWarnings.setManaged(!warnings.isBlank());
        treeWarnings.setVisible(!warnings.isBlank());
        treeWarnings.getStyleClass().removeAll("muted", "warning", "error");
        treeWarnings.getStyleClass().add(warnings.isBlank() ? "muted" : "warning");
        updateWatcherStatus();
        updateMenuStates();
    }

    private void showTreeError(ErrorNotice notice) {
        lastTreeData = null;
        treeRoot.getChildren().clear();
        hideFilterStatus();
        treeWarnings.setText("Error " + notice.code() + ": " + notice.message() + " " + notice.action());
        treeWarnings.setManaged(true);
        treeWarnings.setVisible(true);
        treeWarnings.getStyleClass().removeAll("muted", "warning", "error");
        treeWarnings.getStyleClass().add("error");
        updateMenuStates();
    }

    private void applyTreeFilter() {
        if (lastTreeData == null) {
            hideFilterStatus();
            return;
        }
        renderTree();
    }

    private void renderTree() {
        if (lastTreeData == null) {
            treeRoot.getChildren().clear();
            return;
        }
        String query = searchField == null ? "" : searchField.getText();
        List<TreeData.Node> roots = TreeFilter.filter(TreeModel.roots(lastTreeData), query);

        suppressSelection = true;
        treeRoot.getChildren().setAll(roots.stream().map(this::toItem).toList());
        suppressSelection = false;

        updateFilterStatus(query, roots);
        restoreSelection();
    }

    private void updateFilterStatus(String query, List<TreeData.Node> roots) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            hideFilterStatus();
            return;
        }
        int count = TreeFilter.countVisible(roots);
        filterStatus.setText(count == 0
            ? "Sin coincidencias para «" + normalized + "»"
            : count + (count == 1 ? " coincidencia" : " coincidencias"));
        filterStatus.getStyleClass().removeAll("muted", "warning");
        filterStatus.getStyleClass().add(count == 0 ? "warning" : "muted");
        filterStatus.setManaged(true);
        filterStatus.setVisible(true);
    }

    private void hideFilterStatus() {
        if (filterStatus == null) {
            return;
        }
        filterStatus.setText("");
        filterStatus.setManaged(false);
        filterStatus.setVisible(false);
    }

    private void restoreSelection() {
        if (selectedNode == null) {
            return;
        }
        TreeItem<TreeData.Node> found = findItem(treeRoot, selectedNode);
        suppressSelection = true;
        if (found == null) {
            treeView.getSelectionModel().clearSelection();
        } else {
            treeView.getSelectionModel().select(found);
            expandParents(found);
        }
        suppressSelection = false;
        if (found != null) {
            updateSelectionInfo(selectedNode);
        } else {
            hideSelectionInfo();
        }
    }

    private TreeItem<TreeData.Node> findItem(TreeItem<TreeData.Node> parent, TreeData.Node target) {
        for (TreeItem<TreeData.Node> child : parent.getChildren()) {
            TreeData.Node value = child.getValue();
            if (value != null && sameNode(value, target)) {
                return child;
            }
            TreeItem<TreeData.Node> nested = findItem(child, target);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static boolean sameNode(TreeData.Node left, TreeData.Node right) {
        return Objects.equals(left.path(), right.path())
            && Objects.equals(left.type(), right.type())
            && Objects.equals(left.name(), right.name());
    }

    private static void expandParents(TreeItem<TreeData.Node> item) {
        TreeItem<TreeData.Node> parent = item.getParent();
        while (parent != null) {
            parent.setExpanded(true);
            parent = parent.getParent();
        }
    }

    private static Node treeIcon(TreeData.Node node) {
        String type = node.type() == null ? "" : node.type();
        Icons.Icon icon = switch (type) {
            case "agent" -> Icons.Icon.AGENT;
            case "skill" -> Icons.Icon.SKILL;
            case "hook" -> Icons.Icon.HOOK;
            case "profile" -> Icons.Icon.PROFILE;
            case "provider" -> Icons.Icon.PROVIDER;
            case "module" -> Icons.Icon.MODULE;
            case "technician" -> Icons.Icon.TECHNICIAN;
            case "sdd" -> Icons.Icon.SDD;
            case "root" -> "Global".equals(node.name()) ? Icons.Icon.GLOBE : Icons.Icon.FOLDER;
            default -> Icons.Icon.FOLDER;
        };
        List<String> classes = new ArrayList<>();
        classes.add("tree-icon");
        classes.add("icon-" + (type.isBlank() ? "unknown" : type));
        if (node.pending()) {
            classes.add("icon-pending");
        }
        if ("legacy".equals(node.status())) {
            classes.add("icon-legacy");
        }
        if ("detached".equals(node.status())) {
            classes.add("icon-detached");
        }
        return Icons.of(icon, 15, classes.toArray(String[]::new));
    }

    private TreeItem<TreeData.Node> toItem(TreeData.Node node) {
        TreeItem<TreeData.Node> item = new TreeItem<>(node);
        if (node.children() != null) {
            item.getChildren().setAll(node.children().stream().map(this::toItem).toList());
        }
        boolean filtering = searchField != null && !searchField.getText().isBlank();
        item.setExpanded(filtering || TreeModel.ROOT.equals(node.type()));
        return item;
    }

    private void onSelectNode(TreeData.Node node) {
        if (node == null || node.type() == null || TreeModel.ROOT.equals(node.type())) {
            if (currentProject() == null) {
                showWelcome();
            } else {
                showWorkspace();
                previewPanel.showEmpty();
            }
            return;
        }
        showWorkspace();
        switch (node.type()) {
            case "agent", "skill", "profile" -> loadArtifact(node);
            case "sdd" -> loadSddDirectory(node);
            case "module" -> previewPanel.showMessage(node.name(),
                "Módulo del equipo. Selecciona una de sus skills o agentes para verlo y editarlo.");
            case "technician" -> previewPanel.showMessage(node.name(),
                "Técnico del equipo. Sus artefactos generados aparecen agrupados por proveedor y módulo.");
            case "provider" -> previewPanel.showMessage(node.name(),
                "Proveedor. Selecciona un agente, una skill o un perfil para previsualizarlo.");
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
        beginBusy("Abriendo " + node.name() + "…");
        Thread.ofVirtual().start(() -> {
            try {
                DescribeData data = cliClient.describe(artifact, currentProject());

                if ("skill".equals(data.kind()) && data.files() != null && !data.files().isEmpty()) {
                    loadSkill(data);
                } else {
                    EditorModel editor = EditorModel.of(data, currentProject());
                    Platform.runLater(() -> {
                        previewPanel.showArtifact(ArtifactPreview.of(data), editor);
                        endBusy("Listo");
                    });
                }
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    previewPanel.showError(node.name(), notice.message() + " " + notice.action());
                    endBusy("Error al abrir el artefacto");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    previewPanel.showError(node.name(),
                        exception.getMessage() == null ? "Error inesperado." : exception.getMessage());
                    endBusy("Error al abrir el artefacto");
                });
            }
        });
    }

    private void loadSkill(DescribeData data) {
        try {
            Map<String, String> contents = new HashMap<>();
            Path skillPath = Path.of(data.path());

            for (String file : data.files()) {
                Path filePath = skillPath.resolve(file);
                try {
                    contents.put(file, Files.readString(filePath));
                } catch (IOException exception) {
                    contents.put(file, "");
                }
            }

            SkillEditorModel skillEditor = new SkillEditorModel(data, currentProject(), contents);
            Platform.runLater(() -> {
                previewPanel.showSkill(ArtifactPreview.of(data), skillEditor);
                endBusy("Listo");
            });
        } catch (RuntimeException exception) {
            Platform.runLater(() -> {
                previewPanel.showError(data.name(),
                    exception.getMessage() == null ? "Error inesperado." : exception.getMessage());
                endBusy("Error al abrir la skill");
            });
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
        beginBusy("Leyendo " + file.getFileName() + "…");
        Thread.ofVirtual().start(() -> {
            try {
                String content = SddFiles.read(sddDir, file);
                Platform.runLater(() -> {
                    previewPanel.showSddFile(file.getFileName().toString(), content);
                    endBusy("Listo");
                });
            } catch (SddFiles.SddReadException exception) {
                Platform.runLater(() -> {
                    previewPanel.showError(file.getFileName().toString(), exception.getMessage());
                    endBusy("Error al leer el fichero");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    previewPanel.showError(file.getFileName().toString(),
                        exception.getMessage() == null ? "Error inesperado." : exception.getMessage());
                    endBusy("Error al leer el fichero");
                });
            }
        });
    }

    private void updateSelectionInfo(TreeData.Node node) {
        selectionInfo.setText(formatNode(node));
        selectionInfo.setManaged(true);
        selectionInfo.setVisible(true);
    }

    private void hideSelectionInfo() {
        selectionInfo.setText("");
        selectionInfo.setManaged(false);
        selectionInfo.setVisible(false);
    }

    // ------------------------------------------------------------------
    // Watcher y refresco
    // ------------------------------------------------------------------

    private void onFileSystemChange(ChangeEvent event) {
        Platform.runLater(() -> {
            boolean auto = autoRefreshCheck != null && autoRefreshCheck.isSelected()
                && previewPanel != null && !previewPanel.isDirty();
            if (auto) {
                setStatus("Cambios externos detectados; actualizando…");
                autoRefreshDelay.playFromStart();
            } else if (staleTracker != null) {
                staleTracker.markStale(event.changedPaths());
                updateStaleWarning();
                if (treeView != null) {
                    treeView.refresh();
                }
            }
            if (cacheStore != null) {
                cacheStore.invalidateAll();
            }
        });
    }

    private void autoRefresh() {
        if (previewPanel != null && previewPanel.isDirty()) {
            updateStaleWarning();
            return;
        }
        if (staleTracker != null) {
            staleTracker.clear();
        }
        updateStaleWarning();
        refreshTree();
        if (selectedNode != null && selectedNode.path() != null
            && !TreeModel.ROOT.equals(selectedNode.type())
            && previewPanel != null && !previewPanel.isDirty()) {
            onSelectNode(selectedNode);
        }
    }

    private void updateStaleWarning() {
        if (staleTracker == null || staleBanner == null) {
            return;
        }
        if (staleTracker.isAnyStale()) {
            int count = staleTracker.size();
            staleWarning.setText("Cambios externos detectados (" + count + " fichero"
                + (count == 1 ? "" : "s") + ").");
            staleBanner.setManaged(true);
            staleBanner.setVisible(true);
        } else {
            staleWarning.setText("");
            staleBanner.setManaged(false);
            staleBanner.setVisible(false);
        }
    }

    private void invalidateAfterWrite() {
        if (cacheStore != null) {
            cacheStore.invalidateAll();
        }
        if (staleTracker != null) {
            staleTracker.clear();
            updateStaleWarning();
        }
    }

    private void reloadWithGuard() {
        if (!confirmCloseEditors("Cambios sin guardar",
            "Elige qué hacer antes de recargar el árbol.")) {
            return;
        }
        if (cacheStore != null) {
            cacheStore.invalidateAll();
        }
        if (staleTracker != null) {
            staleTracker.clear();
            updateStaleWarning();
        }
        refreshTree();
        if (selectedNode != null && selectedNode.path() != null
            && !TreeModel.ROOT.equals(selectedNode.type())) {
            onSelectNode(selectedNode);
        }
        setStatus("Recargando…");
    }

    // ------------------------------------------------------------------
    // Proyecto
    // ------------------------------------------------------------------

    private void chooseProject() {
        if (projectSession == null) {
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar proyecto");
        if (projectSession.path() != null) {
            File current = projectSession.path().toFile();
            if (current.isDirectory()) {
                chooser.setInitialDirectory(current);
            }
        }
        File selected = chooser.showDialog(stage);
        if (selected == null) {
            return;
        }
        if (!confirmCloseEditors("Cambiar de proyecto",
            "Hay cambios sin guardar. ¿Qué quieres hacer antes de cambiar de proyecto?")) {
            return;
        }
        loadProject(selected.toPath());
    }

    private void loadProject(Path path) {
        projectPathLabel.setText(path.toString());
        projectPathLabel.setTooltip(new Tooltip(path.toString()));
        setProjectState("Validando…", "muted");
        projectSummaryLabel.setText("");
        projectSummaryLabel.setManaged(false);
        projectSummaryLabel.setVisible(false);
        openProjectButton.setDisable(true);
        initProjectButton.setDisable(true);
        beginBusy("Validando proyecto…");

        Thread.ofVirtual().start(() -> {
            try {
                StatusData status = projectSession.select(path);
                Platform.runLater(() -> {
                    openProjectButton.setDisable(false);
                    renderProject(status);
                    endBusy("Proyecto cargado");
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    openProjectButton.setDisable(false);
                    showProjectError(path, notice);
                    endBusy("Error al validar el proyecto");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    openProjectButton.setDisable(false);
                    showProjectError(path, new ErrorNotice(
                        "client.unexpected", path.toString(), exception.getMessage(), "Reintentar."));
                    endBusy("Error al validar el proyecto");
                });
            }
        });
    }

    private void initProject() {
        if (projectSession == null || projectSession.path() == null) {
            return;
        }
        setProjectState("Iniciando…", "muted");
        openProjectButton.setDisable(true);
        initProjectButton.setDisable(true);
        beginBusy("Iniciando proyecto…");

        Thread.ofVirtual().start(() -> {
            try {
                InitData result = projectSession.init();
                Platform.runLater(() -> {
                    openProjectButton.setDisable(false);
                    renderProject(projectSession.status());
                    setProjectState(result.created() ? "Proyecto iniciado" : "Ya estaba configurado", "ok");
                    endBusy(result.created() ? "Proyecto iniciado" : "El proyecto ya estaba configurado");
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    openProjectButton.setDisable(false);
                    showProjectError(projectSession.path(), notice);
                    endBusy("Error al iniciar el proyecto");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    openProjectButton.setDisable(false);
                    showProjectError(projectSession.path(), new ErrorNotice(
                        "client.unexpected", projectSession.path().toString(),
                        exception.getMessage(), "Reintentar."));
                    endBusy("Error al iniciar el proyecto");
                });
            }
        });
    }

    private void renderProject(StatusData status) {
        projectPathLabel.setText(projectSession.path().toString());
        projectPathLabel.setTooltip(new Tooltip(projectSession.path().toString()));
        boolean configured = projectSession.configured();
        setProjectState(configured ? "Configurado" : "No configurado", configured ? "ok" : "warning");
        String summary = formatProjectSummary(status);
        projectSummaryLabel.setText(summary);
        projectSummaryLabel.setManaged(!summary.isBlank());
        projectSummaryLabel.setVisible(!summary.isBlank());
        initProjectButton.setDisable(configured);
        initProjectButton.setManaged(!configured);
        initProjectButton.setVisible(!configured);
        createMenuButton.setDisable(cliClient == null);
        startWatcherForProject();
        showWorkspace();
        updateWelcomeState();
        updateMenuStates();
        updateWindowTitle();
        refreshTree();
    }

    private void startWatcherForProject() {
        if (projectWatcher == null || projectSession == null || projectSession.path() == null) {
            return;
        }
        List<Path> directories = List.of(projectSession.path());
        staleTracker.clear();
        cacheStore.invalidateAll();
        updateStaleWarning();
        projectWatcher.start(directories);
        updateWatcherStatus();
    }

    private void closeProject() {
        if (!confirmCloseEditors("Cerrar proyecto",
            "Hay cambios sin guardar. ¿Qué quieres hacer antes de cerrar el proyecto?")) {
            return;
        }
        if (projectWatcher != null) {
            projectWatcher.stop();
        }
        if (staleTracker != null) {
            staleTracker.clear();
            updateStaleWarning();
        }
        if (projectSession != null) {
            projectSession.clear();
        }
        lastTreeData = null;
        selectedNode = null;
        treeRoot.getChildren().clear();
        if (searchField != null) {
            searchField.clear();
        }
        hideFilterStatus();
        treeWarnings.setText("");
        treeWarnings.setManaged(false);
        treeWarnings.setVisible(false);
        hideSelectionInfo();
        projectPathLabel.setText("Ningún proyecto seleccionado");
        projectPathLabel.setTooltip(null);
        projectSummaryLabel.setText("");
        projectSummaryLabel.setManaged(false);
        projectSummaryLabel.setVisible(false);
        setProjectState("Sin proyecto", "muted");
        initProjectButton.setManaged(false);
        initProjectButton.setVisible(false);
        previewPanel.showEmpty();
        showWelcome();
        updateWelcomeState();
        updateWatcherStatus();
        updateMenuStates();
        updateWindowTitle();
        refreshTree();
        setStatus("Proyecto cerrado");
    }

    private void showProjectError(Path path, ErrorNotice notice) {
        projectPathLabel.setText(path == null ? "" : path.toString());
        projectPathLabel.setTooltip(path == null ? null : new Tooltip(path.toString()));
        setProjectState("Error " + notice.code(), "error");
        projectSummaryLabel.setText("Mensaje: " + notice.message() + "\nAcción: " + notice.action());
        projectSummaryLabel.setManaged(true);
        projectSummaryLabel.setVisible(true);
        initProjectButton.setDisable(true);
        initProjectButton.setManaged(true);
        initProjectButton.setVisible(true);
        createMenuButton.setDisable(true);
        showWelcome();
        updateWelcomeState();
        updateMenuStates();
        updateWindowTitle();
    }

    private void setProjectState(String text, String style) {
        projectStateChip.setText(text);
        projectStateChip.getStyleClass().removeAll("muted", "ok", "error", "warning");
        projectStateChip.getStyleClass().add(style);
    }

    private void updateWelcomeState() {
        if (welcomePane == null) {
            return;
        }
        welcomePane.setCli(cliClient != null, cliError);
        Path path = projectSession == null ? null : projectSession.path();
        boolean configured = projectSession != null && projectSession.configured();
        welcomePane.setProject(path, configured);
    }

    private void showWelcome() {
        if (welcomePane == null || workspaceTabs == null) {
            return;
        }
        welcomePane.setManaged(true);
        welcomePane.setVisible(true);
        workspaceTabs.setManaged(false);
        workspaceTabs.setVisible(false);
    }

    private void showWorkspace() {
        if (welcomePane == null || workspaceTabs == null) {
            return;
        }
        welcomePane.setManaged(false);
        welcomePane.setVisible(false);
        workspaceTabs.setManaged(true);
        workspaceTabs.setVisible(true);
    }

    private void loadGlobalStatus() {
        if (cliClient == null) {
            return;
        }
        Thread.ofVirtual().start(() -> {
            try {
                StatusData status = cliClient.status(null);
                Platform.runLater(() -> {
                    globalStatus = status;
                    updateMenuStates();
                });
            } catch (RuntimeException ignored) {
                // Sin estado global: las acciones dependientes del proyecto siguen disponibles.
            }
        });
    }

    private List<String> providerIds() {
        StatusData status = projectSession != null && projectSession.status() != null
            ? projectSession.status()
            : globalStatus;
        if (status == null || status.providers() == null) {
            return List.of();
        }
        return status.providers().stream()
            .map(StatusData.Provider::id)
            .toList();
    }

    // ------------------------------------------------------------------
    // Edición y guardado
    // ------------------------------------------------------------------

    private void saveCurrent() {
        if (previewPanel.isSkillMode()) {
            SkillEditorModel skillEditor = previewPanel.skillEditor();
            if (skillEditor != null && skillEditor.isDirty()) {
                saveAllSkillFiles(skillEditor);
            }
            return;
        }
        EditorModel editor = previewPanel.editor();
        if (editor != null && editor.isDirty()) {
            if (!editor.headerDirty() && !editor.bodyDirty() && editor.sharedBodyDirty()) {
                propagate(editor);
            } else {
                saveEditorAsync(editor);
            }
        }
    }

    private boolean guardNavigation(TreeItem<TreeData.Node> previous, TreeData.Node node) {
        return confirmCloseEditors("Cambios sin guardar",
            "Elige qué hacer antes de cambiar de selección.");
    }

    private boolean confirmCloseEditors(String title, String text) {
        EditorModel editor = previewPanel.editor();
        if (editor != null && editor.isDirty() && !confirmSaveForAgent(editor, title, text)) {
            return false;
        }
        SkillEditorModel skillEditor = previewPanel.skillEditor();
        if (skillEditor != null && skillEditor.isDirty() && !confirmSaveForSkill(skillEditor, title, text)) {
            return false;
        }
        return true;
    }

    private boolean confirmCloseApplication() {
        return confirmCloseEditors("Cerrar la aplicación",
            "Hay cambios sin guardar. ¿Qué quieres hacer antes de salir?");
    }

    private boolean confirmSaveForAgent(EditorModel editor, String title, String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText("El artefacto tiene cambios sin guardar.");
        alert.setContentText(text);
        Theme.apply(alert);
        ButtonType saveButton = new ButtonType("Guardar", ButtonBar.ButtonData.YES);
        ButtonType discardButton = new ButtonType("Descartar", ButtonBar.ButtonData.NO);
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

    private boolean confirmSaveForSkill(SkillEditorModel skillEditor, String title, String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText("La skill tiene ficheros con cambios sin guardar.");
        alert.setContentText(text);
        Theme.apply(alert);
        ButtonType saveButton = new ButtonType("Guardar todo", ButtonBar.ButtonData.YES);
        ButtonType discardButton = new ButtonType("Descartar", ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancelButton) {
            return false;
        }
        if (result.get() == saveButton) {
            String error = saveAllSkillFilesNow(skillEditor);
            if (error != null) {
                previewPanel.showSaveStatus("Error al guardar: " + error, true);
                return false;
            }
            skillEditor.markAllSaved();
            previewPanel.showSaveStatus("Todos los ficheros guardados.", false);
            invalidateAfterWrite();
        }
        return true;
    }

    private void saveEditorAsync(EditorModel editor) {
        beginBusy("Guardando…");
        Thread.ofVirtual().start(() -> {
            SaveOutcome outcome = saveEditor(editor);
            Platform.runLater(() -> {
                applySaveOutcome(editor, outcome);
                endBusy(outcome.ok() ? "Guardado" : "Error al guardar");
            });
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
            invalidateAfterWrite();
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
            invalidateAfterWrite();
            return null;
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            return notice.message() + " " + notice.action();
        } catch (RuntimeException exception) {
            return exception.getMessage() == null ? "Error inesperado." : exception.getMessage();
        }
    }

    private void saveSkillFile(SkillEditorModel skillEditor, String file) {
        String content = skillEditor.getContent(file);
        beginBusy("Guardando " + file + "…");
        Thread.ofVirtual().start(() -> {
            try {
                cliClient.saveBody(skillEditor.skillPath(), content, skillEditor.project(), file);
                Platform.runLater(() -> {
                    skillEditor.markSaved(file);
                    previewPanel.showSaveStatus("Fichero " + file + " guardado.", false);
                    invalidateAfterWrite();
                    endBusy("Guardado");
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
                    endBusy("Error al guardar");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    previewPanel.showSaveStatus("Error: "
                        + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
                    endBusy("Error al guardar");
                });
            }
        });
    }

    private void saveAllSkillFiles(SkillEditorModel skillEditor) {
        List<String> dirtyFiles = skillEditor.files().stream().filter(skillEditor::isDirty).toList();
        if (dirtyFiles.isEmpty()) {
            return;
        }
        beginBusy("Guardando " + dirtyFiles.size() + " fichero(s)…");
        Thread.ofVirtual().start(() -> {
            String error = saveAllSkillFilesNow(skillEditor, dirtyFiles);
            Platform.runLater(() -> {
                if (error == null) {
                    dirtyFiles.forEach(skillEditor::markSaved);
                    previewPanel.showSaveStatus("Todos los ficheros guardados.", false);
                    invalidateAfterWrite();
                    endBusy("Guardado");
                } else {
                    previewPanel.showSaveStatus("Error: " + error, true);
                    endBusy("Error al guardar");
                }
            });
        });
    }

    private String saveAllSkillFilesNow(SkillEditorModel skillEditor) {
        List<String> dirtyFiles = skillEditor.files().stream().filter(skillEditor::isDirty).toList();
        return saveAllSkillFilesNow(skillEditor, dirtyFiles);
    }

    private String saveAllSkillFilesNow(SkillEditorModel skillEditor, List<String> dirtyFiles) {
        for (String file : dirtyFiles) {
            try {
                cliClient.saveBody(skillEditor.skillPath(), skillEditor.getContent(file),
                    skillEditor.project(), file);
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                return notice.message() + " " + notice.action();
            } catch (RuntimeException exception) {
                return exception.getMessage() == null ? "Error inesperado." : exception.getMessage();
            }
        }
        return null;
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
            invalidateAfterWrite();
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

    /** Vincula un artefacto legacy o desvinculado a su grupo: simula, muestra el diff y lo aplica. */
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
            invalidateAfterWrite();
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

    private void reloadSelectedArtifact() {
        if (selectedNode != null && !TreeModel.ROOT.equals(selectedNode.type())) {
            onSelectNode(selectedNode);
        }
    }

    /** Propaga el contenido de un fichero de skill a otras instancias del mismo grupo. */
    private void propagateSkillFile(SkillEditorModel skillEditor, String file) {
        String content = skillEditor.getContent(file);
        beginBusy("Calculando cambios…");
        Thread.ofVirtual().start(() -> {
            try {
                SaveData dryRun = cliClient.saveGroup(skillEditor.skillPath(), content, true,
                    skillEditor.project(), file);
                Platform.runLater(() -> {
                    endBusy("Listo");
                    if (!dryRun.written()) {
                        previewPanel.showSaveStatus("Sin cambios que propagar.", false);
                        return;
                    }
                    boolean confirmed = DiffReviewDialog.confirm(stage, "Propagar cambios",
                        "Se actualizarán las siguientes copias de la skill:", DiffReview.summary(dryRun));
                    if (!confirmed) {
                        previewPanel.showSaveStatus("Propagación cancelada.", false);
                        return;
                    }
                    beginBusy("Propagando…");
                    Thread.ofVirtual().start(() -> executePropagateSkillFile(skillEditor, file, content));
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
                    endBusy("Error al propagar");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    previewPanel.showSaveStatus(
                        "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
                    endBusy("Error al propagar");
                });
            }
        });
    }

    private void executePropagateSkillFile(SkillEditorModel skillEditor, String file, String content) {
        try {
            SaveData result = cliClient.saveGroup(skillEditor.skillPath(), content, false,
                skillEditor.project(), file);
            Platform.runLater(() -> {
                skillEditor.markSaved(file);
                previewPanel.showSaveStatus("Propagado a " + result.members().size() + " copias.", false);
                invalidateAfterWrite();
                refreshTree();
                reloadSelectedArtifact();
                endBusy("Propagado");
            });
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            Platform.runLater(() -> {
                previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
                endBusy("Error al propagar");
            });
        } catch (RuntimeException exception) {
            Platform.runLater(() -> {
                previewPanel.showSaveStatus(
                    "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
                endBusy("Error al propagar");
            });
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

    // ------------------------------------------------------------------
    // Creación
    // ------------------------------------------------------------------

    private void createModule() {
        if (!requireProject()) {
            return;
        }

        var result = CreateModuleDialog.show(stage, providerIds());
        if (result.isEmpty()) {
            return;
        }

        CreateModuleDialog.Result params = result.get();
        if (params.name().isBlank() || params.technicians().isEmpty() || params.providers().isEmpty()) {
            showErrorAlert("Faltan datos", null, "Completa nombre, técnicos y proveedores.");
            return;
        }

        beginBusy("Simulando el módulo…");
        Thread.ofVirtual().start(() -> {
            try {
                CreateModuleData dryRun = cliClient.createModule(
                    projectSession.path(), params.name(), params.technicians(), params.providers(), true);

                Platform.runLater(() -> {
                    endBusy("Listo");
                    boolean confirmed = CreationPreviewDialog.confirm(
                        stage, "Crear módulo", "Se registrará el módulo y se generarán sus artefactos.", dryRun);
                    if (!confirmed) {
                        return;
                    }
                    beginBusy("Creando el módulo…");
                    Thread.ofVirtual().start(() -> executeCreateModule(
                        params.name(), params.technicians(), params.providers()));
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    showErrorAlert("Error", null, notice.message());
                    endBusy("Error al simular el módulo");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    showErrorAlert("Error", null, exception.getMessage());
                    endBusy("Error al simular el módulo");
                });
            }
        });
    }

    private void executeCreateModule(String name, List<String> technicians, List<String> providers) {
        try {
            CreateModuleData result = cliClient.createModule(
                projectSession.path(), name, technicians, providers, false);

            Platform.runLater(() -> {
                showInfoAlert("Módulo creado",
                    "Se crearon " + result.created().size() + " artefactos.\n"
                        + "Se omitieron " + result.skipped().size() + ".");
                endBusy("Módulo creado");
                refreshAll();
            });
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            Platform.runLater(() -> {
                showErrorAlert("Error", null, notice.message());
                endBusy("Error al crear el módulo");
            });
        } catch (RuntimeException exception) {
            Platform.runLater(() -> {
                showErrorAlert("Error", null, exception.getMessage());
                endBusy("Error al crear el módulo");
            });
        }
    }

    private void addTechnician() {
        if (!requireProject()) {
            return;
        }
        if (projectSession.status().project().modules().isEmpty()) {
            showErrorAlert("Sin módulos", null, "No hay módulos registrados en el proyecto.");
            return;
        }

        String selectedModule = projectSession.status().project().modules().getFirst();
        var result = AddTechnicianDialog.show(stage, selectedModule, providerIds());
        if (result.isEmpty()) {
            return;
        }

        AddTechnicianDialog.Result params = result.get();
        if (params.technicians().isEmpty() || params.providers().isEmpty()) {
            showErrorAlert("Faltan datos", null, "Selecciona técnicos y proveedores.");
            return;
        }

        beginBusy("Simulando los técnicos…");
        Thread.ofVirtual().start(() -> {
            try {
                CreateModuleData dryRun = cliClient.addTechnician(
                    projectSession.path(), selectedModule, params.technicians(), params.providers(), true);

                Platform.runLater(() -> {
                    endBusy("Listo");
                    boolean confirmed = CreationPreviewDialog.confirm(
                        stage, "Añadir técnicos",
                        "Se generarán los artefactos para los técnicos en «" + selectedModule + "».", dryRun);
                    if (!confirmed) {
                        return;
                    }
                    beginBusy("Añadiendo técnicos…");
                    Thread.ofVirtual().start(() -> executeAddTechnician(
                        selectedModule, params.technicians(), params.providers()));
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    showErrorAlert("Error", null, notice.message());
                    endBusy("Error al simular los técnicos");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    showErrorAlert("Error", null, exception.getMessage());
                    endBusy("Error al simular los técnicos");
                });
            }
        });
    }

    private void executeAddTechnician(String module, List<String> technicians, List<String> providers) {
        try {
            CreateModuleData result = cliClient.addTechnician(
                projectSession.path(), module, technicians, providers, false);

            Platform.runLater(() -> {
                showInfoAlert("Técnicos añadidos",
                    "Se crearon " + result.created().size() + " artefactos.\n"
                        + "Se omitieron " + result.skipped().size() + ".");
                endBusy("Técnicos añadidos");
                refreshAll();
            });
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            Platform.runLater(() -> {
                showErrorAlert("Error", null, notice.message());
                endBusy("Error al añadir técnicos");
            });
        } catch (RuntimeException exception) {
            Platform.runLater(() -> {
                showErrorAlert("Error", null, exception.getMessage());
                endBusy("Error al añadir técnicos");
            });
        }
    }

    private void createArtifact() {
        boolean hasProject = projectSession != null && projectSession.configured();
        List<String> providers = providerIds();
        if (providers.isEmpty()) {
            showErrorAlert("Sin proveedores", null, "No hay proveedores configurados.");
            return;
        }

        Optional<CreateArtifactDialog.Result> selectionOpt = CreateArtifactDialog.show(stage, providers, hasProject);
        if (selectionOpt.isEmpty()) {
            return;
        }
        CreateArtifactDialog.Result selection = selectionOpt.get();

        if ("project".equals(selection.scope()) && !hasProject) {
            showErrorAlert("Proyecto requerido", null,
                "No hay proyecto configurado. Selecciona un proyecto o usa ámbito global.");
            return;
        }

        beginBusy("Consultando el catálogo…");
        Thread.ofVirtual().start(() -> {
            try {
                CatalogData catalogList = cliClient.catalog(
                    selection.provider(), selection.scope(), selection.kind(), null,
                    "project".equals(selection.scope()) ? projectSession.path() : null);

                if (catalogList.entries().isEmpty()) {
                    Platform.runLater(() -> {
                        showErrorAlert("Sin artefactos", null,
                            "No hay artefactos disponibles en el catálogo para esta combinación.");
                        endBusy("Sin resultados");
                    });
                    return;
                }

                Platform.runLater(() -> {
                    endBusy("Listo");
                    Optional<String> nameOpt = selectNameFromCatalog(catalogList);
                    if (nameOpt.isEmpty()) {
                        return;
                    }
                    String name = nameOpt.get();
                    beginBusy("Cargando la plantilla…");

                    Thread.ofVirtual().start(() -> {
                        try {
                            CatalogData catalogWithTemplate = cliClient.catalog(
                                selection.provider(), selection.scope(), selection.kind(), name,
                                "project".equals(selection.scope()) ? projectSession.path() : null);

                            if (catalogWithTemplate.entries().isEmpty()) {
                                Platform.runLater(() -> {
                                    showErrorAlert("Error", null,
                                        "No se encontró la plantilla para «" + name + "».");
                                    endBusy("Sin plantilla");
                                });
                                return;
                            }

                            CatalogData.CatalogEntry entry = catalogWithTemplate.entries().getFirst();
                            Platform.runLater(() -> {
                                endBusy("Listo");
                                Optional<EditTemplateDialog.Result> editOpt = EditTemplateDialog.show(
                                    stage, name, entry.template(), entry.exists());
                                if (editOpt.isEmpty()) {
                                    return;
                                }
                                beginBusy("Creando el artefacto…");
                                Thread.ofVirtual().start(() -> executeCreateArtifact(
                                    selection, name, editOpt.get(), entry));
                            });
                        } catch (CliClientException exception) {
                            ErrorNotice notice = ErrorPresenter.present(exception);
                            Platform.runLater(() -> {
                                showErrorAlert("Error", null, notice.message());
                                endBusy("Error al cargar la plantilla");
                            });
                        } catch (RuntimeException exception) {
                            Platform.runLater(() -> {
                                showErrorAlert("Error", null, exception.getMessage());
                                endBusy("Error al cargar la plantilla");
                            });
                        }
                    });
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    showErrorAlert("Error", null, notice.message());
                    endBusy("Error al consultar el catálogo");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    showErrorAlert("Error", null, exception.getMessage());
                    endBusy("Error al consultar el catálogo");
                });
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
        Theme.apply(dialog);
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
                    showInfoAlert("Artefacto creado",
                        "El artefacto «" + name + "» se creó sobrescribiendo el existente.\n"
                            + "Ruta: " + result.path());
                } else {
                    showInfoAlert("Artefacto creado",
                        "El artefacto «" + name + "» se creó correctamente.\n"
                            + "Ruta: " + result.path());
                }
                endBusy("Artefacto creado");
                refreshAll();
            });
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            Platform.runLater(() -> {
                showErrorAlert("Error", null, notice.message());
                endBusy("Error al crear el artefacto");
            });
        } catch (RuntimeException exception) {
            Platform.runLater(() -> {
                showErrorAlert("Error", null, exception.getMessage());
                endBusy("Error al crear el artefacto");
            });
        }
    }

    private boolean requireProject() {
        if (projectSession == null || projectSession.path() == null) {
            showErrorAlert("Sin proyecto", null, "Selecciona un proyecto primero.");
            return false;
        }
        if (!projectSession.configured()) {
            showErrorAlert("Proyecto no configurado", null,
                "Inicia el proyecto antes de crear módulos o técnicos.");
            return false;
        }
        return true;
    }

    private void refreshAll() {
        try {
            StatusData status = projectSession.select(projectSession.path());
            Platform.runLater(() -> renderProject(status));
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            Platform.runLater(() -> showErrorAlert("Error al refrescar", null, notice.message()));
        } catch (RuntimeException exception) {
            Platform.runLater(() -> showErrorAlert("Error al refrescar", null, exception.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // Acciones de interfaz
    // ------------------------------------------------------------------

    private void onEditorDirtyChanged() {
        boolean dirty = previewPanel != null && previewPanel.isDirty();
        if (dirtyStatus != null) {
            dirtyStatus.setText(dirty ? "● Cambios sin guardar" : "");
            dirtyStatus.setManaged(dirty);
            dirtyStatus.setVisible(dirty);
        }
        if (treeView != null) {
            treeView.refresh();
        }
        updateWindowTitle();
        updateMenuStates();
    }

    private void onConsoleStateChanged(boolean active) {
        if (consoleTab != null) {
            consoleTab.setText(active ? "Consola  ●" : "Consola");
        }
    }

    private void focusSearch() {
        if (searchField == null) {
            return;
        }
        searchField.requestFocus();
        searchField.selectAll();
    }

    private void showConsole() {
        if (workspaceTabs == null || consoleTab == null) {
            return;
        }
        showWorkspace();
        workspaceTabs.getSelectionModel().select(consoleTab);
        if (ptyConsolePanel != null) {
            ptyConsolePanel.focusInput();
        }
    }

    private void updateMenuStates() {
        boolean cli = cliClient != null;
        boolean hasProject = projectSession != null && projectSession.path() != null;
        boolean configured = projectSession != null && projectSession.configured();
        boolean hasProviders = !providerIds().isEmpty();
        boolean hasModules = hasProject && hasModules(projectSession.status());
        boolean dirty = previewPanel != null && previewPanel.isDirty();

        if (openProjectMenuItem != null) {
            openProjectMenuItem.setDisable(!cli);
        }
        if (reloadMenuItem != null) {
            reloadMenuItem.setDisable(!cli);
        }
        if (saveMenuItem != null) {
            saveMenuItem.setDisable(!dirty);
        }
        if (closeProjectMenuItem != null) {
            closeProjectMenuItem.setDisable(!hasProject);
        }
        if (initProjectMenuItem != null) {
            initProjectMenuItem.setDisable(!cli || !hasProject || configured);
        }
        setDisabled(createModuleItems, !cli || !configured);
        setDisabled(addTechnicianItems, !cli || !hasModules);
        setDisabled(createArtifactItems, !cli || !hasProviders);
        if (searchMenuItem != null) {
            searchMenuItem.setDisable(lastTreeData == null);
        }
        if (consoleMenuItem != null) {
            consoleMenuItem.setDisable(ptyConsolePanel == null);
        }
        if (openProjectButton != null) {
            openProjectButton.setDisable(!cli);
        }
        if (createMenuButton != null) {
            createMenuButton.setDisable(!cli || !hasProviders);
        }
        if (sidebarRefreshButton != null) {
            sidebarRefreshButton.setDisable(!cli);
        }
        if (searchField != null) {
            searchField.setDisable(!cli);
        }
        if (initProjectButton != null) {
            boolean showInit = hasProject && !configured;
            initProjectButton.setManaged(showInit);
            initProjectButton.setVisible(showInit);
            initProjectButton.setDisable(!cli || configured);
        }
        if (welcomePane != null) {
            welcomePane.setProvidersAvailable(hasProviders);
        }
    }

    private static void setDisabled(List<MenuItem> items, boolean disabled) {
        for (MenuItem item : items) {
            item.setDisable(disabled);
        }
    }

    private void updateWatcherStatus() {
        if (watcherStatus == null) {
            return;
        }
        boolean running = projectWatcher != null && projectWatcher.isRunning();
        watcherStatus.setText(running ? "Vigilando cambios" : "Watcher inactivo");
        watcherStatus.getStyleClass().removeAll("ok", "muted");
        watcherStatus.getStyleClass().add(running ? "ok" : "muted");
    }

    private void updateWindowTitle() {
        if (stage == null) {
            return;
        }
        String project = "";
        if (projectSession != null && projectSession.path() != null) {
            Path fileName = projectSession.path().getFileName();
            project = " — " + (fileName == null ? projectSession.path() : fileName);
        }
        String dirty = previewPanel != null && previewPanel.isDirty() ? " •" : "";
        stage.setTitle("Agentic Viewer" + project + dirty);
    }

    private void setStatus(String message) {
        if (statusMessage != null && busyCount == 0) {
            statusMessage.setText(message);
        }
    }

    private void beginBusy(String message) {
        busyCount++;
        if (busySpinner != null) {
            busySpinner.setManaged(true);
            busySpinner.setVisible(true);
        }
        if (statusMessage != null) {
            statusMessage.setText(message);
        }
    }

    private void endBusy(String message) {
        busyCount = Math.max(0, busyCount - 1);
        if (busyCount == 0) {
            if (busySpinner != null) {
                busySpinner.setManaged(false);
                busySpinner.setVisible(false);
            }
            if (statusMessage != null) {
                statusMessage.setText(message == null ? "Listo" : message);
            }
        }
    }

    private void requestExit() {
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void closeResources() {
        if (projectWatcher != null) {
            projectWatcher.close();
        }
        if (ptyConsolePanel != null) {
            ptyConsolePanel.shutdown();
        }
    }

    private boolean hasModules(StatusData status) {
        return status != null
            && status.project() != null
            && status.project().modules() != null
            && !status.project().modules().isEmpty();
    }

    private Path currentProject() {
        return projectSession == null ? null : projectSession.path();
    }

    // ------------------------------------------------------------------
    // Diálogos de información y formato
    // ------------------------------------------------------------------

    private void showShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("Atajos de teclado");
        alert.setHeaderText("Trabaja sin soltar el teclado");
        Theme.apply(alert);
        alert.setContentText("""
            Ctrl+O        Abrir proyecto
            F5            Recargar el árbol
            Ctrl+S        Guardar (en skills, todos los ficheros)
            Ctrl+F        Buscar artefactos
            F6            Ir a la consola
            Ctrl+M        Crear módulo
            Ctrl+Shift+T  Añadir técnico
            Ctrl+N        Crear artefacto
            Ctrl+Q        Salir
            """);
        alert.showAndWait();
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("Acerca de Agentic Viewer");
        alert.setHeaderText("Agentic Viewer");
        Theme.apply(alert);
        alert.setContentText("""
            Visor de escritorio para el entorno agéntico de desarrollo.

            JavaFX 21 · Consume el CLI mediante el contrato cli-json v1.
            Todo se ejecuta en tu equipo; no requiere conexión a internet.
            """);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.setResizable(true);
        Theme.apply(alert);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.setResizable(true);
        Theme.apply(alert);
        alert.showAndWait();
    }

    private static String formatProjectSummary(StatusData status) {
        StringBuilder text = new StringBuilder();
        if (status.providers() != null && !status.providers().isEmpty()) {
            text.append("Proveedores: ")
                .append(status.providers().stream()
                    .map(provider -> provider.name() + " = " + provider.state())
                    .collect(Collectors.joining(" · ")));
        }
        if (status.project() != null) {
            if (!text.isEmpty()) {
                text.append("   ·   ");
            }
            text.append(status.project().initialized() ? "Proyecto con .sdd" : "Proyecto sin .sdd");
            int modules = status.project().modules() == null ? 0 : status.project().modules().size();
            text.append(" · módulos: ").append(modules);
            if (status.project().warnings() != null && !status.project().warnings().isEmpty()) {
                text.append(" · avisos: ").append(status.project().warnings().size());
            }
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
        text.append(kindLabel(node.type())).append(" · ").append(node.name());
        if (node.provider() != null) {
            text.append("\nProveedor: ").append(node.provider());
        }
        if (node.status() != null) {
            text.append(" · Estado: ").append(node.status());
        }
        if (node.pending()) {
            text.append(" · Pendiente");
        }
        if (node.path() != null) {
            text.append('\n').append(node.path());
        }
        return text.toString();
    }

    private static String kindLabel(String type) {
        if (type == null) {
            return "Nodo";
        }
        return switch (type) {
            case "agent" -> "Agente";
            case "skill" -> "Skill";
            case "profile" -> "Perfil";
            case "module" -> "Módulo";
            case "technician" -> "Técnico";
            case "sdd" -> "Directorio .sdd";
            case "provider" -> "Proveedor";
            case "root" -> "Raíz";
            default -> type;
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
