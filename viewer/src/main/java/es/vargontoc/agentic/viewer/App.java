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
import es.vargontoc.agentic.viewer.cli.model.CliVersionInfo;
import es.vargontoc.agentic.viewer.cli.model.ConfigureProviderData;
import es.vargontoc.agentic.viewer.cli.model.CreateArtifactData;
import es.vargontoc.agentic.viewer.cli.model.CreateModuleData;
import es.vargontoc.agentic.viewer.cli.model.DescribeData;
import es.vargontoc.agentic.viewer.cli.model.InitData;
import es.vargontoc.agentic.viewer.cli.model.SaveData;
import es.vargontoc.agentic.viewer.cli.model.StatusData;
import es.vargontoc.agentic.viewer.cli.model.TeamData;
import es.vargontoc.agentic.viewer.cli.model.TeamUpdateData;
import es.vargontoc.agentic.viewer.cli.model.TeamUpdateRequest;
import es.vargontoc.agentic.viewer.cli.model.TreeData;
import es.vargontoc.agentic.viewer.compat.VersionStatus;
import es.vargontoc.agentic.viewer.creation.AddTechnicianDialog;
import es.vargontoc.agentic.viewer.creation.CreateArtifactWizard;
import es.vargontoc.agentic.viewer.creation.CreateModuleDialog;
import es.vargontoc.agentic.viewer.creation.CreationPreviewDialog;
import es.vargontoc.agentic.viewer.diff.ConflictResolutionDialog;
import es.vargontoc.agentic.viewer.diff.DiffReview;
import es.vargontoc.agentic.viewer.diff.DiffReviewDialog;
import es.vargontoc.agentic.viewer.draft.DraftStore;
import es.vargontoc.agentic.viewer.editor.EditorModel;
import es.vargontoc.agentic.viewer.editor.SkillEditorModel;
import es.vargontoc.agentic.viewer.preview.ArtifactPreview;
import es.vargontoc.agentic.viewer.preview.NodeDetails;
import es.vargontoc.agentic.viewer.preview.PreviewPanel;
import es.vargontoc.agentic.viewer.preview.SddFiles;
import es.vargontoc.agentic.viewer.runtime.AppPaths;
import es.vargontoc.agentic.viewer.runtime.RecentProjectsStore;
import es.vargontoc.agentic.viewer.runtime.ViewerLog;
import es.vargontoc.agentic.viewer.runtime.ViewerVersion;
import es.vargontoc.agentic.viewer.runtime.WatcherPaths;
import es.vargontoc.agentic.viewer.setup.ConfigureProvidersDialog;
import es.vargontoc.agentic.viewer.terminal.PtyConsolePanel;
import es.vargontoc.agentic.viewer.team.TeamEditorDialog;
import es.vargontoc.agentic.viewer.tree.TreeFilter;
import es.vargontoc.agentic.viewer.tree.TreeGroups;
import es.vargontoc.agentic.viewer.tree.TreeLabels;
import es.vargontoc.agentic.viewer.tree.TreeModel;
import es.vargontoc.agentic.viewer.ui.Icons;
import es.vargontoc.agentic.viewer.ui.StatusBarView;
import es.vargontoc.agentic.viewer.ui.Theme;
import es.vargontoc.agentic.viewer.ui.ToastHost;
import es.vargontoc.agentic.viewer.ui.TreePresentation;
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
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
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
    private boolean initialProviderCheckDone = false;
    private String cliError;

    private ProjectWatcher projectWatcher;
    private CacheStore cacheStore;
    private StaleTracker staleTracker;
    private DraftStore draftStore;
    private RecentProjectsStore recentProjects;
    private PauseTransition draftSaveDelay;
    private String lastDraftPath;

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
    private MenuItem discardDraftsMenuItem;
    private final List<MenuItem> createModuleItems = new ArrayList<>();
    private final List<MenuItem> addTechnicianItems = new ArrayList<>();
    private final List<MenuItem> createArtifactItems = new ArrayList<>();
    private MenuItem searchMenuItem;
    private MenuItem consoleMenuItem;
    private MenuItem configureProvidersMenuItem;
    private Menu recentProjectsMenu;
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

    private StatusBarView statusBar;
    private ToastHost toastHost;
    private PauseTransition autoRefreshDelay;
    private int busyCount;

    private VersionStatus versionStatus;
    private HBox versionBanner;
    private Label versionBannerLabel;
    private String lastVersionWarning;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        initServices();

        previewPanel = new PreviewPanel();
        previewPanel.setOnSddFileSelected(this::loadSddFile);
        previewPanel.setOnSave(this::saveEditorAsync);
        previewPanel.setOnPropagate(this::propagateAsync);
        previewPanel.setOnAdopt(this::adoptAsync);
        previewPanel.setOnSaveSkillFile(this::saveSkillFile);
        previewPanel.setOnSaveAllSkillFiles(this::saveAllSkillFiles);
        previewPanel.setOnPropagateSkillFile(this::propagateSkillFile);
        previewPanel.setOnDirtyChanged(this::onEditorDirtyChanged);
        previewPanel.setOnContentChanged(this::scheduleDraftSave);

        ptyConsolePanel = new PtyConsolePanel(cliCommand, this::currentProject);
        ptyConsolePanel.setOnStateChanged(this::onConsoleStateChanged);

        welcomePane = new WelcomePane();
        welcomePane.setVersionInfo("Visor " + ViewerVersion.current(), false);
        welcomePane.setOnOpenProject(this::chooseProject);
        welcomePane.setOnInitProject(this::initProject);
        welcomePane.setOnCreateArtifact(this::createArtifact);
        welcomePane.setOnOpenRecent(this::openRecentProject);

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

        draftSaveDelay = new PauseTransition(Duration.millis(1200));
        draftSaveDelay.setOnFinished(event -> saveDraftNow());

        VBox top = new VBox(menuBar(), headerBar(), versionBanner());
        SplitPane body = new SplitPane(sidebar(), centerHost);
        body.getStyleClass().add("main-split");
        body.setDividerPositions(0.27);
        SplitPane.setResizableWithParent(body.getItems().getFirst(), false);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app");
        root.setTop(top);
        root.setCenter(body);
        root.setBottom(statusBar());

        toastHost = new ToastHost();
        StackPane sceneRoot = new StackPane(root, toastHost);
        Scene scene = new Scene(sceneRoot, 1320, 880);
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
        ViewerLog.info("Ventana mostrada");

        updateWelcomeState();
        updateMenuStates();
        updateWatcherStatus();
        updateWindowTitle();
        showWelcome();
        loadVersions(null);

        if (cliError != null) {
            welcomePane.setCli(false, cliError);
            showErrorAlert("Error de aplicación", "No se encontró el CLI", cliError);
        } else {
            welcomePane.setCli(true, null);
            refreshTree();
            loadGlobalStatus();
            startWatcherForProject();
            refreshDraftStatus();
            reopenLastProject();
            ViewerLog.info("Vista inicial preparada");
        }
    }

    private void initServices() {
        try {
            cliCommand = CliLocator.resolve(Path.of("").toAbsolutePath());
            ViewerLog.info("CLI en uso: " + cliCommand.cliJar());
            cacheStore = new CacheStore();
            cliClient = new CliClient(new CliProcess(cliCommand), new ObjectMapper(), cacheStore);
            projectSession = new ProjectSession(cliClient);
            staleTracker = new StaleTracker();
            projectWatcher = new ProjectWatcher(this::onFileSystemChange);
            draftStore = DraftStore.defaultStore();
            recentProjects = RecentProjectsStore.defaultStore();
            ViewerLog.info("Visor iniciado. Log: " + ViewerLog.file());
        } catch (RuntimeException exception) {
            cliError = exception.getMessage();
            ViewerLog.error("No se pudo inicializar el visor", exception);
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
        recentProjectsMenu = new Menu("Abrir recientes");
        configureProvidersMenuItem = menuItem("Configurar proveedores…", null, Icons.Icon.SETTINGS, this::configureProviders);
        reloadMenuItem = menuItem("Recargar", "F5", Icons.Icon.REFRESH, this::reloadWithGuard);
        saveMenuItem = menuItem("Guardar", "Shortcut+S", Icons.Icon.SAVE, this::saveCurrent);
        discardDraftsMenuItem = menuItem("Descartar borradores", null, Icons.Icon.TRASH, this::discardDrafts);
        closeProjectMenuItem = menuItem("Cerrar proyecto", null, Icons.Icon.CLOSE, this::closeProject);
        MenuItem exit = menuItem("Salir", "Shortcut+Q", Icons.Icon.LOG_OUT, this::requestExit);
        file.getItems().addAll(openProjectMenuItem, recentProjectsMenu, new SeparatorMenuItem(),
            configureProvidersMenuItem, reloadMenuItem, saveMenuItem,
            discardDraftsMenuItem, new SeparatorMenuItem(), closeProjectMenuItem,
            new SeparatorMenuItem(), exit);

        Menu project = new Menu("Proyecto");
        initProjectMenuItem = menuItem("Iniciar proyecto", null, Icons.Icon.PLAY, this::initProject);
        MenuItem createModule = menuItem("Crear módulo…", "Shortcut+M", Icons.Icon.MODULE, this::createModule);
        MenuItem addTechnician = menuItem("Añadir técnico…", "Shortcut+Shift+T", Icons.Icon.TECHNICIAN, this::addTechnician);
        MenuItem createArtifact = menuItem("Crear artefactos…", "Shortcut+N", Icons.Icon.PLUS, this::createArtifact);
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
            menuItem("Comprobar versiones…", null, Icons.Icon.INFO, () -> loadVersions(this::showAbout)),
            menuItem("Abrir carpeta de logs", null, Icons.Icon.FOLDER_OPEN, this::openLogFolder),
            menuItem("Acerca de", null, Icons.Icon.INFO, this::showAbout));

        MenuBar bar = new MenuBar(file, project, view, help);
        bar.getStyleClass().add("app-menu");
        refreshRecentProjectsMenu();
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
        MenuItem quickArtifact = new MenuItem("Artefactos…");
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
                setGraphic(TreePresentation.icon(node));
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
                boolean stale = staleTracker != null && node.path() != null && staleTracker.isStale(node.path());
                if (stale) {
                    getStyleClass().add("tree-stale");
                }
                setTooltip(new Tooltip(TreePresentation.tooltip(node, dirty, stale)));
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
            if (node != null && TreeGroups.GROUP.equals(node.type()) && selected != null) {
                selected.setExpanded(!selected.isExpanded());
            }
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

    private StatusBarView statusBar() {
        statusBar = new StatusBarView(autoRefreshMenuItem, this::showAbout);
        statusBar.setVersions("Visor " + ViewerVersion.current() + " · comprobando CLI…",
            "Versiones del visor y del CLI", false);
        return statusBar;
    }

    /** Aviso persistente y no modal cuando el contrato del CLI no coincide. */
    private Node versionBanner() {
        versionBannerLabel = new Label();
        versionBannerLabel.getStyleClass().add("version-banner-text");
        versionBannerLabel.setWrapText(true);
        HBox.setHgrow(versionBannerLabel, Priority.ALWAYS);

        Button showVersions = new Button("Ver versiones");
        showVersions.getStyleClass().addAll("secondary", "small");
        showVersions.setGraphic(Icons.of(Icons.Icon.INFO, 13, "button-icon"));
        showVersions.setOnAction(event -> showAbout());

        versionBanner = new HBox(8, versionBannerLabel, showVersions);
        versionBanner.setAlignment(Pos.CENTER_LEFT);
        versionBanner.getStyleClass().add("version-banner");
        versionBanner.setManaged(false);
        versionBanner.setVisible(false);
        return versionBanner;
    }

    // ------------------------------------------------------------------
    // Versiones y compatibilidad del contrato
    // ------------------------------------------------------------------

    private void loadVersions(Runnable onDone) {
        String viewerVersion = ViewerVersion.current();
        int expected = CliClient.EXPECTED_SCHEMA_VERSION;

        if (cliClient == null) {
            applyVersions(VersionStatus.unavailable(viewerVersion, expected, cliError), onDone);
            return;
        }

        if (statusBar != null) {
            statusBar.setVersions("Visor " + viewerVersion + " · comprobando CLI…",
                "Versiones del visor y del CLI", false);
        }
        Thread.ofVirtual().start(() -> {
            VersionStatus status;
            try {
                CliVersionInfo info = cliClient.version();
                status = info.schemaVersion() == expected
                    ? VersionStatus.compatible(viewerVersion, info.version(), expected)
                    : VersionStatus.mismatch(viewerVersion, info.version(), expected, info.schemaVersion());
            } catch (RuntimeException exception) {
                status = VersionStatus.unavailable(viewerVersion, expected, exception.getMessage());
            }
            VersionStatus result = status;
            Platform.runLater(() -> applyVersions(result, onDone));
        });
    }

    private void applyVersions(VersionStatus status, Runnable onDone) {
        versionStatus = status;
        if (statusBar != null) {
            statusBar.setVersions(status.summary(), status.aboutText(), !status.compatible());
        }
        if (welcomePane != null) {
            welcomePane.setVersionInfo(status.summary(), !status.compatible());
        }

        boolean mismatch = status.state() == VersionStatus.State.CONTRACT_MISMATCH;
        if (versionBanner != null) {
            versionBanner.setManaged(mismatch);
            versionBanner.setVisible(mismatch);
            if (mismatch) {
                versionBannerLabel.setText(status.warning());
            }
        }
        if (mismatch && !Objects.equals(lastVersionWarning, status.warning())) {
            lastVersionWarning = status.warning();
            notifyWarning("Contrato incompatible", status.warning());
        }
        if (!status.compatible()) {
            ViewerLog.warn("Compatibilidad: " + status.summary()
                + (status.detail().isBlank() ? "" : " · " + status.detail()));
        }
        if (onDone != null) {
            onDone.run();
        }
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
        ViewerLog.info("Árbol actualizado: " + treeRoot.getChildren().size() + " raíces");
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
        ViewerLog.warn("Error de árbol " + notice.code() + ": " + notice.message());
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
        List<TreeData.Node> grouped = TreeGroups.group(TreeModel.roots(lastTreeData));
        List<TreeData.Node> roots = TreeFilter.filter(grouped, query);

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
            case "hook" -> loadHook(node);
            case "sdd" -> loadSddDirectory(node);
            case "module" -> loadModuleDetail(node);
            case "technician" -> showTechnicianDetail(node);
            case "provider" -> previewPanel.showMessage(node.name(),
                "Proveedor. Selecciona un agente, una skill o un perfil para previsualizarlo.");
            case "group" -> {
                int count = node.children() == null ? 0 : node.children().size();
                previewPanel.showMessage(node.name(), count == 1
                    ? "1 artefacto. Selecciónalo para verlo."
                    : count + " artefactos. Selecciona uno para verlo.");
            }
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
                    DraftStore.Draft draft = findDraft(data.path());
                    if (draft != null) {
                        applyDraft(editor, draft);
                        ViewerLog.info("Borrador recuperado para " + data.path());
                    }
                    Platform.runLater(() -> {
                        previewPanel.showArtifact(ArtifactPreview.of(data), editor);
                        if (draft != null) {
                            previewPanel.showSaveStatus("Borrador recuperado de la sesión anterior.", false);
                        }
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

    /**
     * Ficha del hook {@code sdd-specs-only}: estado, ruta y acción de instalación.
     * El CLI lo entrega como nodo {@code hook} bajo el proveedor global.
     */
    private void loadModuleDetail(TreeData.Node node) {
        previewPanel.showLoading(node.name());
        beginBusy("Cargando módulo…");
        Path project = currentProject();
        Thread.ofVirtual().start(() -> {
            TeamData team = null;
            if (project != null && cliClient != null) {
                try {
                    team = cliClient.team(project);
                } catch (RuntimeException ignored) {
                    // Sin team.md legible se muestra lo que ya trae el árbol.
                }
            }
            final TeamData data = team;
            Platform.runLater(() -> {
                if (selectedNode != node) {
                    endBusy("Listo");
                    return;
                }
                previewPanel.showNodeDetail(NodeDetails.module(node, data), Icons.Icon.MODULE);
                endBusy("Listo");
            });
        });
    }

    private void showTechnicianDetail(TreeData.Node node) {
        previewPanel.showNodeDetail(NodeDetails.technician(node, parentOf(node)), Icons.Icon.TECHNICIAN);
    }

    private TreeData.Node parentOf(TreeData.Node target) {
        if (lastTreeData == null || target == null) {
            return null;
        }
        TreeData.Node parent = parentOf(lastTreeData.project(), target);
        return parent != null ? parent : parentOf(lastTreeData.global(), target);
    }

    private static TreeData.Node parentOf(List<TreeData.Node> nodes, TreeData.Node target) {
        if (nodes == null) {
            return null;
        }
        for (TreeData.Node node : nodes) {
            if (node.children() == null) {
                continue;
            }
            for (TreeData.Node child : node.children()) {
                if (child == target) {
                    return node;
                }
            }
            TreeData.Node deeper = parentOf(node.children(), target);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }

    private void loadHook(TreeData.Node node) {
        String provider = node.provider() == null ? "el proveedor" : node.provider();
        String statusText = node.pending()
            ? "pendiente de instalar"
            : TreePresentation.statusLabel(node.status());
        String message;
        if (node.pending()) {
            message = "El hook sdd-specs-only no está instalado en " + provider
                + ". Al instalarlo se restringen las escrituras de los agentes SDD.";
        } else if ("legacy".equals(node.status())) {
            message = "El hook está instalado en " + provider
                + " pero está modificado o no tiene registro. Se respeta tal cual y no se sobrescribe.";
        } else {
            message = "El hook está instalado en " + provider
                + " y coincide con la versión de la aplicación.";
        }
        message += "\n\nLa acción ejecuta configure-provider: instala o actualiza el hook y completa"
            + " los artefactos globales que falten, sin sobrescribir los existentes.";
        String actionLabel = node.pending() ? "Instalar hook" : "Reinstalar / actualizar hook";
        previewPanel.showNodeInfo("sdd-specs-only", message, statusText, node.path(), actionLabel,
            () -> installHook(provider));
    }

    private void installHook(String providerId) {
        if (cliClient == null) {
            showErrorAlert("Error de aplicación", "No se encontró el CLI",
                "No se puede instalar el hook sin el CLI de la aplicación.");
            return;
        }
        beginBusy("Instalando el hook…");
        Thread.ofVirtual().start(() -> {
            try {
                ConfigureProviderData data = cliClient.configureProvider(providerId);
                Platform.runLater(() -> {
                    invalidateAfterWrite();
                    refreshAll();
                    String warnings = data.warnings() == null ? "" : String.join("\n- ", data.warnings());
                    String message = "Proveedor: " + providerId
                        + "\nCreados: " + size(data.created())
                        + " · Omitidos: " + size(data.skipped());
                    boolean hasWarnings = !warnings.isBlank();
                    if (hasWarnings) {
                        message += "\n\nAvisos:\n- " + warnings;
                    }
                    endBusy("Hook actualizado");
                    if (hasWarnings) {
                        notifyWarning("Hook sdd-specs-only", message);
                    } else {
                        notifySuccess("Hook sdd-specs-only", message);
                    }
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    showErrorAlert("Error", null, notice.message());
                    endBusy("Error al instalar el hook");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    showErrorAlert("Error", null,
                        exception.getMessage() == null ? "Error inesperado." : exception.getMessage());
                    endBusy("Error al instalar el hook");
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
            DraftStore.Draft draft = findDraft(data.path());
            if (draft != null) {
                for (Map.Entry<String, String> entry : draft.files().entrySet()) {
                    skillEditor.setContent(entry.getKey(), entry.getValue());
                }
                ViewerLog.info("Borrador recuperado para " + data.path());
            }
            Platform.runLater(() -> {
                previewPanel.showSkill(ArtifactPreview.of(data), skillEditor);
                if (draft != null) {
                    previewPanel.showSaveStatus("Borrador recuperado de la sesión anterior.", false);
                }
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
                    if ("team.md".equals(file.getFileName().toString())) {
                        Path project = sddDir.getParent();
                        previewPanel.showTeamFile(file.getFileName().toString(), content,
                            () -> openTeamEditor(project));
                    } else {
                        previewPanel.showSddFile(file.getFileName().toString(), content);
                    }
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

    private void openTeamEditor(Path project) {
        if (cliClient == null || project == null) {
            return;
        }
        beginBusy("Leyendo el equipo…");
        Thread.ofVirtual().start(() -> {
            try {
                TeamData data = cliClient.team(project);
                Platform.runLater(() -> {
                    endBusy("Listo");
                    TeamEditorDialog.show(stage, data)
                        .ifPresent(request -> saveTeam(project, data, request));
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    showErrorAlert("Error al leer el equipo", null,
                        notice.message() + " " + notice.action());
                    endBusy("Error al leer el equipo");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    showErrorAlert("Error al leer el equipo", null, unexpectedMessage(exception));
                    endBusy("Error al leer el equipo");
                });
            }
        });
    }

    private void saveTeam(Path project, TeamData original, TeamUpdateRequest request) {
        beginBusy("Simulando cambios del equipo…");
        Thread.ofVirtual().start(() -> {
            try {
                TeamUpdateData dryRun = cliClient.teamUpdate(project, request, original.hash(), true);
                Platform.runLater(() -> {
                    endBusy("Listo");
                    if (!dryRun.written()) {
                        notifySuccess("Equipo del proyecto", "No hay cambios que guardar.");
                        return;
                    }
                    boolean confirmed = DiffReviewDialog.confirm(stage, "Guardar equipo",
                        "Revisa los cambios de .sdd/team.md antes de guardar.", dryRun.diff());
                    if (!confirmed) {
                        return;
                    }
                    beginBusy("Guardando el equipo…");
                    Thread.ofVirtual().start(() -> saveTeamConfirmed(project, original, request));
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    showErrorAlert("Error al simular el equipo", null, notice.message() + " " + notice.action());
                    endBusy("Error al simular el equipo");
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    showErrorAlert("Error al simular el equipo", null, unexpectedMessage(exception));
                    endBusy("Error al simular el equipo");
                });
            }
        });
    }

    private void saveTeamConfirmed(Path project, TeamData original, TeamUpdateRequest request) {
        try {
            TeamUpdateData result = cliClient.teamUpdate(project, request, original.hash(), false);
            Platform.runLater(() -> {
                invalidateAfterWrite();
                refreshAll();
                loadSddFile(project.resolve(".sdd").resolve("team.md"));
                StringBuilder message = new StringBuilder("Módulos: ").append(request.modules().size());
                if (result.backup() != null) {
                    message.append("\nCopia de seguridad: ").append(result.backup());
                }
                boolean hasWarnings = result.warnings() != null && !result.warnings().isEmpty();
                if (hasWarnings) {
                    message.append("\n\nAvisos:");
                    result.warnings().forEach(w -> message.append("\n· ").append(w.type())
                        .append(w.line() > 0 ? " (línea " + w.line() + ")" : ""));
                }
                endBusy("Equipo guardado");
                if (hasWarnings) {
                    notifyWarning("Equipo guardado", message.toString());
                } else {
                    notifySuccess("Equipo guardado", message.toString());
                }
            });
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            Platform.runLater(() -> {
                showErrorAlert("Error al guardar el equipo", null, notice.message() + " " + notice.action());
                endBusy("Error al guardar el equipo");
            });
        } catch (RuntimeException exception) {
            Platform.runLater(() -> {
                showErrorAlert("Error al guardar el equipo", null, unexpectedMessage(exception));
                endBusy("Error al guardar el equipo");
            });
        }
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
            boolean auto = statusBar != null && statusBar.isAutoRefreshSelected()
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
        TreeData.Node keep = selectedNode;
        boolean reloadPreview = statusBar == null || statusBar.isAutoRefreshPreviewSelected();
        if (staleTracker != null) {
            staleTracker.clear();
        }
        updateStaleWarning();
        refreshTree();
        if (keep != null && keep.path() != null
            && !TreeModel.ROOT.equals(keep.type())
            && previewPanel != null && !previewPanel.isDirty()) {
            if (reloadPreview) {
                onSelectNode(keep);
            } else if (staleTracker != null) {
                staleTracker.markStale(List.of(Path.of(keep.path())));
                updateStaleWarning();
                staleWarning.setText(
                    "El árbol está actualizado, pero la vista abierta no. Pulsa «Recargar ahora» para actualizarla.");
            }
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
            String project = currentProject() == null ? null : currentProject().toString();
            cacheStore.invalidateProject(project);
            if (project != null) {
                cacheStore.invalidateProject(null);
            }
        }
        if (staleTracker != null) {
            staleTracker.clear();
            if (Platform.isFxApplicationThread()) {
                updateStaleWarning();
            } else {
                Platform.runLater(this::updateStaleWarning);
            }
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
        if (cacheStore != null) {
            cacheStore.invalidateAll();
        }
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
                    rememberRecentProject(projectSession.path());
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

    private void openRecentProject(Path path) {
        if (path == null || projectSession == null) {
            return;
        }
        if (!Files.isDirectory(path)) {
            recentProjects.remove(path);
            refreshRecentProjectsMenu();
            updateWelcomeState();
            showErrorAlert("Proyecto no disponible", null,
                "La carpeta del proyecto ya no existe:\n" + path);
            return;
        }
        if (path.equals(currentProject()) && projectSession.status() != null) {
            showWorkspace();
            return;
        }
        if (!confirmCloseEditors("Cambiar de proyecto",
            "Hay cambios sin guardar. ¿Qué quieres hacer antes de cambiar de proyecto?")) {
            return;
        }
        loadProject(path);
    }

    private void rememberRecentProject(Path path) {
        if (recentProjects == null || path == null) {
            return;
        }
        recentProjects.remember(path);
        refreshRecentProjectsMenu();
    }

    private void refreshRecentProjectsMenu() {
        if (recentProjectsMenu == null) {
            return;
        }
        recentProjectsMenu.getItems().clear();
        List<Path> recents = recentProjects == null ? List.of() : recentProjects.list();
        if (recents.isEmpty()) {
            MenuItem empty = new MenuItem("Sin proyectos recientes");
            empty.setDisable(true);
            recentProjectsMenu.getItems().add(empty);
            return;
        }
        for (Path path : recents) {
            MenuItem item = new MenuItem(path.toString());
            item.setOnAction(event -> openRecentProject(path));
            recentProjectsMenu.getItems().add(item);
        }
        recentProjectsMenu.getItems().add(new SeparatorMenuItem());
        MenuItem clear = new MenuItem("Limpiar lista");
        clear.setOnAction(event -> {
            if (recentProjects != null) {
                recentProjects.clear();
            }
            refreshRecentProjectsMenu();
            updateWelcomeState();
        });
        recentProjectsMenu.getItems().add(clear);
    }

    private void reopenLastProject() {
        if (recentProjects == null) {
            return;
        }
        Optional<Path> last = recentProjects.lastExisting();
        if (last.isEmpty()) {
            return;
        }
        ViewerLog.info("Reabriendo último proyecto: " + last.get());
        loadProject(last.get());
    }

    private void initProject() {
        if (projectSession == null || projectSession.path() == null) {
            return;
        }
        invalidateAfterWrite();
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

    private void configureProviders() {
        if (cliClient == null) {
            showErrorAlert("Error de aplicación", "No se encontró el CLI",
                "No se puede configurar proveedores sin el CLI de la aplicación.");
            return;
        }

        List<String> current = globalStatus != null && globalStatus.providers() != null
            ? globalStatus.providers().stream().map(StatusData.Provider::id).toList()
            : List.of();

        var result = ConfigureProvidersDialog.show(stage, cliClient, current);
        result.ifPresent(r -> {
            // Mostrar resultados
            ConfigureProvidersDialog.showResults(stage, r);
            // Recargar estado global para actualizar la lista de proveedores
            loadGlobalStatus();
            refreshTree();
            ViewerLog.info("Proveedores configurados: " + r.results().size());
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
        if (projectWatcher == null) {
            return;
        }
        Path project = projectSession == null ? null : projectSession.path();
        List<Path> directories = WatcherPaths.directories(project, Path.of(System.getProperty("user.home")));
        if (cacheStore != null) {
            cacheStore.invalidateAll();
        }
        if (staleTracker != null) {
            staleTracker.clear();
            updateStaleWarning();
        }
        if (directories.isEmpty()) {
            projectWatcher.stop();
            updateWatcherStatus();
            return;
        }
        ViewerLog.info("Watcher: " + directories.size() + " directorio(s)");
        Thread.ofVirtual().start(() -> {
            projectWatcher.start(directories);
            Platform.runLater(this::updateWatcherStatus);
        });
    }

    private void closeProject() {
        if (!confirmCloseEditors("Cerrar proyecto",
            "Hay cambios sin guardar. ¿Qué quieres hacer antes de cerrar el proyecto?")) {
            return;
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
        startWatcherForProject();
        setStatus("Proyecto cerrado");
    }

    private void showProjectError(Path path, ErrorNotice notice) {
        ViewerLog.warn("Error de proyecto " + notice.code() + ": " + notice.message());
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
        welcomePane.setRecentProjects(recentProjects == null ? List.of() : recentProjects.list());
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
                    // Asistente de primer arranque: si no hay proveedores configurados,
                    // ofrecer configurar uno. Solo se muestra la primera vez.
                    if (!initialProviderCheckDone) {
                        initialProviderCheckDone = true;
                        boolean noProviders = status == null || status.providers() == null 
                            || status.providers().isEmpty()
                            || status.providers().stream().allMatch(p -> {
                                String state = p.state();
                                return "not_installed".equals(state) || "unconfigured".equals(state);
                            });
                        if (noProviders) {
                            ViewerLog.info("No hay proveedores configurados, mostrando asistente de primer arranque");
                            Platform.runLater(this::configureProviders);
                        }
                    }
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
                propagateAsync(editor);
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
            SaveOutcome outcome = resolveEditorConflicts(editor);
            applySaveOutcome(editor, outcome);
            if (!outcome.ok()) {
                return false;
            }
            if (editor.sharedBodyDirty() && !propagateNow(editor)) {
                return false;
            }
        } else if (result.get() == discardButton) {
            if (draftSaveDelay != null) {
                draftSaveDelay.stop();
            }
            deleteDraft(editor.path() == null ? lastDraftPath : editor.path().toString());
            lastDraftPath = null;
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
        } else if (result.get() == discardButton) {
            if (draftSaveDelay != null) {
                draftSaveDelay.stop();
            }
            deleteDraft(skillEditor.skillPath() == null ? lastDraftPath : skillEditor.skillPath().toString());
            lastDraftPath = null;
        }
        return true;
    }

    private void saveEditorAsync(EditorModel editor) {
        beginBusy("Guardando…");
        Thread.ofVirtual().start(() -> {
            SaveOutcome outcome = saveEditor(editor);
            Platform.runLater(() -> {
                SaveOutcome finalOutcome = outcome.conflictPart() == null
                    ? outcome
                    : resolveEditorConflicts(editor, outcome);
                applySaveOutcome(editor, finalOutcome);
                endBusy(finalOutcome.ok() ? "Guardado" : "Error al guardar");
            });
        });
    }

    /**
     * Intenta guardar las partes sucias sin abrir diálogos: se puede llamar desde
     * un hilo virtual. Si hay conflicto devuelve {@link SavePart} para que la
     * interfaz lo resuelva en el hilo de JavaFX.
     */
    private SaveOutcome saveEditor(EditorModel editor) {
        String message = null;
        if (editor.headerDirty()) {
            SaveOutcome header = saveHeaderAttempt(editor, false);
            if (!header.ok()) {
                return header;
            }
            message = header.message();
        }
        if (editor.bodyDirty()) {
            SaveOutcome body = saveBodyAttempt(editor, false);
            if (!body.ok()) {
                return body;
            }
            if (body.message() != null) {
                message = body.message();
            }
        }
        return SaveOutcome.success(message);
    }

    private SaveOutcome saveHeaderAttempt(EditorModel editor, boolean overwrite) {
        try {
            SaveData data = cliClient.saveHeader(editor.path(), editor.header(), editor.project(), overwrite);
            editor.markHeaderSaved();
            invalidateAfterWrite();
            return SaveOutcome.success(backupMessage(data));
        } catch (CliClientException exception) {
            if (isConflict(exception)) {
                return SaveOutcome.conflict(SavePart.HEADER, exception.getMessage());
            }
            ErrorNotice notice = ErrorPresenter.present(exception);
            return SaveOutcome.failure(notice.message() + " " + notice.action());
        } catch (RuntimeException exception) {
            return SaveOutcome.failure(unexpectedMessage(exception));
        }
    }

    private SaveOutcome saveBodyAttempt(EditorModel editor, boolean overwrite) {
        try {
            SaveData data = cliClient.saveBody(editor.path(), editor.body(), editor.project(), null, overwrite);
            editor.markBodySaved();
            invalidateAfterWrite();
            return SaveOutcome.success(backupMessage(data));
        } catch (CliClientException exception) {
            if (isConflict(exception)) {
                return SaveOutcome.conflict(SavePart.BODY, exception.getMessage());
            }
            ErrorNotice notice = ErrorPresenter.present(exception);
            return SaveOutcome.failure(notice.message() + " " + notice.action());
        } catch (RuntimeException exception) {
            return SaveOutcome.failure(unexpectedMessage(exception));
        }
    }

    /**
     * Resuelve los conflictos de una instancia en el hilo de JavaFX: muestra el
     * diff y aplica «conservar mi versión» o «usar la del disco». Repite el ciclo
     * mientras el fichero vuelva a cambiar entre la elección y la escritura.
     */
    private SaveOutcome resolveEditorConflicts(EditorModel editor) {
        return resolveEditorConflicts(editor, saveEditor(editor));
    }

    /**
     * Resuelve los conflictos de una instancia en el hilo de JavaFX: muestra el
     * diff y aplica «conservar mi versión» o «usar la del disco». Repite el ciclo
     * mientras el fichero vuelva a cambiar entre la elección y la escritura.
     */
    private SaveOutcome resolveEditorConflicts(EditorModel editor, SaveOutcome initial) {
        SaveOutcome current = initial;
        String lastMessage = null;
        while (true) {
            if (current.ok()) {
                return SaveOutcome.success(current.message() != null ? current.message() : lastMessage);
            }
            if (current.conflictPart() == null) {
                return current;
            }
            SaveOutcome resolution = resolveEditorPart(editor, current.conflictPart());
            if (!resolution.ok()) {
                return resolution;
            }
            if (resolution.message() != null) {
                lastMessage = resolution.message();
            }
            current = saveEditor(editor);
        }
    }

    private SaveOutcome resolveEditorPart(EditorModel editor, SavePart part) {
        while (true) {
            DescribeData disk;
            try {
                disk = cliClient.describe(editor.path(), editor.project());
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                return SaveOutcome.failure(
                    "No se pudo leer la versión del disco: " + notice.message() + " " + notice.action());
            } catch (RuntimeException exception) {
                return SaveOutcome.failure("No se pudo leer la versión del disco: " + unexpectedMessage(exception));
            }

            String mine = part == SavePart.HEADER ? editor.header() : editor.body();
            String diskText = part == SavePart.HEADER ? disk.header() : disk.body();
            var choice = ConflictResolutionDialog.show(stage, new ConflictResolutionDialog.Request(
                artifactName(editor), part == SavePart.HEADER ? "Cabecera" : "Cuerpo",
                nullToEmpty(mine), nullToEmpty(diskText)));

            switch (choice) {
                case CANCEL -> {
                    return SaveOutcome.failure("Guardado cancelado: el artefacto cambió fuera de la aplicación.");
                }
                case USE_DISK -> {
                    if (part == SavePart.HEADER) {
                        editor.setHeader(diskText);
                        editor.markHeaderSaved();
                    } else {
                        editor.setBody(diskText);
                        editor.markBodySaved();
                    }
                    refreshSharedBodyFromDisk(editor, disk);
                    previewPanel.refreshAfterExternalResolution();
                    deleteDraft(editor.path() == null ? lastDraftPath : editor.path().toString());
                    return SaveOutcome.success("Se usó la versión del disco.");
                }
                case KEEP_MINE -> {
                    SaveOutcome retry = part == SavePart.HEADER
                        ? saveHeaderAttempt(editor, true)
                        : saveBodyAttempt(editor, true);
                    if (retry.ok() || retry.conflictPart() == null) {
                        return retry;
                    }
                    // El fichero volvió a cambiar: se muestra el conflicto otra vez con el disco actualizado.
                }
            }
        }
    }

    private void saveSkillFile(SkillEditorModel skillEditor, String file) {
        beginBusy("Guardando " + file + "…");
        Thread.ofVirtual().start(() -> {
            SaveOutcome outcome = saveSkillFileAttempt(skillEditor, file, false);
            Platform.runLater(() -> {
                SaveOutcome finalOutcome = outcome.conflictPart() == null
                    ? outcome
                    : resolveSkillFileConflict(skillEditor, file);
                if (finalOutcome.ok()) {
                    skillEditor.markSaved(file);
                    previewPanel.refreshAfterExternalResolution();
                    previewPanel.showSaveStatus(finalOutcome.message() == null
                        ? "Fichero " + file + " guardado."
                        : finalOutcome.message(), false);
                    invalidateAfterWrite();
                } else {
                    previewPanel.showSaveStatus("Error: " + finalOutcome.message(), true);
                }
                endBusy(finalOutcome.ok() ? "Guardado" : "Error al guardar");
            });
        });
    }

    private void saveAllSkillFiles(SkillEditorModel skillEditor) {
        List<String> dirtyFiles = skillEditor.files().stream().filter(skillEditor::isDirty).toList();
        if (dirtyFiles.isEmpty()) {
            return;
        }
        beginBusy("Guardando " + dirtyFiles.size() + " fichero(s)…");
        Thread.ofVirtual().start(() -> {
            SkillBatch batch = saveSkillBatch(skillEditor, dirtyFiles);
            Platform.runLater(() -> {
                batch.saved().forEach(skillEditor::markSaved);
                if (batch.conflictFile() != null) {
                    SaveOutcome resolved = resolveSkillFileConflict(skillEditor, batch.conflictFile());
                    if (!resolved.ok()) {
                        previewPanel.showSaveStatus("Error: " + resolved.message(), true);
                        endBusy("Error al guardar");
                        return;
                    }
                    skillEditor.markSaved(batch.conflictFile());
                    previewPanel.refreshAfterExternalResolution();
                    previewPanel.showSaveStatus(resolved.message() == null
                        ? "Conflicto resuelto." : resolved.message(), false);
                    invalidateAfterWrite();
                    endBusy("Guardado");
                    if (skillEditor.isDirty()) {
                        saveAllSkillFiles(skillEditor);
                    }
                    return;
                }
                if (batch.message() != null) {
                    previewPanel.showSaveStatus("Error: " + batch.message(), true);
                    endBusy("Error al guardar");
                    return;
                }
                previewPanel.showSaveStatus("Todos los ficheros guardados.", false);
                invalidateAfterWrite();
                endBusy("Guardado");
            });
        });
    }

    private SaveOutcome saveSkillFileAttempt(SkillEditorModel skillEditor, String file, boolean overwrite) {
        try {
            SaveData data = cliClient.saveBody(skillEditor.skillPath(), skillEditor.getContent(file),
                skillEditor.project(), file, overwrite);
            invalidateAfterWrite();
            return SaveOutcome.success(backupMessage(data));
        } catch (CliClientException exception) {
            if (isConflict(exception)) {
                return SaveOutcome.conflict(SavePart.BODY, exception.getMessage());
            }
            ErrorNotice notice = ErrorPresenter.present(exception);
            return SaveOutcome.failure(notice.message() + " " + notice.action());
        } catch (RuntimeException exception) {
            return SaveOutcome.failure(unexpectedMessage(exception));
        }
    }

    private SaveOutcome resolveSkillFileConflict(SkillEditorModel skillEditor, String file) {
        while (true) {
            String disk = readSkillFile(skillEditor.skillPath(), file);
            if (disk == null) {
                return SaveOutcome.failure("No se pudo leer la versión del disco de " + file + ".");
            }
            var choice = ConflictResolutionDialog.show(stage, new ConflictResolutionDialog.Request(
                skillName(skillEditor), file, skillEditor.getContent(file), disk));

            switch (choice) {
                case CANCEL -> {
                    return SaveOutcome.failure("Guardado cancelado: " + file + " cambió fuera de la aplicación.");
                }
                case USE_DISK -> {
                    skillEditor.setContent(file, disk);
                    skillEditor.markSaved(file);
                    previewPanel.refreshAfterExternalResolution();
                    deleteDraft(skillEditor.skillPath() == null ? lastDraftPath : skillEditor.skillPath().toString());
                    return SaveOutcome.success("Se usó la versión del disco para " + file + ".");
                }
                case KEEP_MINE -> {
                    SaveOutcome retry = saveSkillFileAttempt(skillEditor, file, true);
                    if (retry.ok() || retry.conflictPart() == null) {
                        return retry;
                    }
                    // El fichero volvió a cambiar: se muestra el conflicto otra vez.
                }
            }
        }
    }

    private SkillBatch saveSkillBatch(SkillEditorModel skillEditor, List<String> dirtyFiles) {
        List<String> saved = new ArrayList<>();
        for (String file : dirtyFiles) {
            SaveOutcome outcome = saveSkillFileAttempt(skillEditor, file, false);
            if (outcome.ok()) {
                saved.add(file);
                continue;
            }
            if (outcome.conflictPart() != null) {
                return SkillBatch.conflict(saved, file);
            }
            return SkillBatch.error(saved, outcome.message());
        }
        return SkillBatch.success(saved);
    }

    /** Guardado síncrono con guía de conflictos, para las confirmaciones de cierre/navegación. */
    private String saveAllSkillFilesNow(SkillEditorModel skillEditor) {
        while (true) {
            List<String> dirtyFiles = skillEditor.files().stream().filter(skillEditor::isDirty).toList();
            if (dirtyFiles.isEmpty()) {
                return null;
            }
            SkillBatch batch = saveSkillBatch(skillEditor, dirtyFiles);
            batch.saved().forEach(skillEditor::markSaved);
            if (batch.conflictFile() != null) {
                SaveOutcome resolved = resolveSkillFileConflict(skillEditor, batch.conflictFile());
                if (!resolved.ok()) {
                    return resolved.message();
                }
                skillEditor.markSaved(batch.conflictFile());
                continue;
            }
            if (batch.message() != null) {
                return batch.message();
            }
            return null;
        }
    }

    private boolean isConflict(CliClientException exception) {
        return exception != null && "conflict.artifact.modified_externally".equals(exception.code());
    }

    private static String unexpectedMessage(Exception exception) {
        return exception.getMessage() == null ? "Error inesperado." : exception.getMessage();
    }

    private static String backupMessage(SaveData data) {
        if (data == null || data.backups() == null || data.backups().isEmpty()) {
            return null;
        }
        return "Guardado · copia de seguridad: " + String.join(", ", data.backups());
    }

    private static String artifactName(EditorModel editor) {
        if (editor.path() == null) {
            return "Artefacto";
        }
        return editor.path().getFileName() == null ? editor.path().toString() : editor.path().getFileName().toString();
    }

    private static String skillName(SkillEditorModel skillEditor) {
        if (skillEditor.skillPath() == null) {
            return "Skill";
        }
        return skillEditor.skillPath().getFileName() == null
            ? skillEditor.skillPath().toString()
            : skillEditor.skillPath().getFileName().toString();
    }

    private String readSkillFile(Path skillPath, String file) {
        if (skillPath == null || file == null || file.isBlank()) {
            return null;
        }
        try {
            Path root = skillPath.toAbsolutePath().normalize();
            Path target = root.resolve(file).normalize();
            if (!target.startsWith(root)) {
                showErrorAlert("Error", null, "La ruta del fichero sale de la skill: " + file);
                return null;
            }
            return Files.readString(target);
        } catch (IOException | RuntimeException exception) {
            showErrorAlert("Error", null,
                "No se pudo leer " + file + ": " + unexpectedMessage(exception));
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Si el cuerpo compartido no tiene cambios propios, se sincroniza con el disco. */
    private static void refreshSharedBodyFromDisk(EditorModel editor, DescribeData disk) {
        if (!editor.sharedBodyDirty() && disk.sharedBody() != null) {
            editor.setSharedBody(disk.sharedBody());
            editor.markSharedBodySaved();
        }
    }

    private enum SavePart { HEADER, BODY }

    private record SkillBatch(List<String> saved, String conflictFile, String message) {
        static SkillBatch success(List<String> saved) {
            return new SkillBatch(saved, null, null);
        }

        static SkillBatch conflict(List<String> saved, String conflictFile) {
            return new SkillBatch(saved, conflictFile, null);
        }

        static SkillBatch error(List<String> saved, String message) {
            return new SkillBatch(saved, null, message);
        }
    }

    /** Propagación síncrona reservada para las confirmaciones de cierre/navegación. */
    private boolean propagateNow(EditorModel editor) {
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
            ViewerLog.error("Error al propagar", exception);
            previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
            return false;
        } catch (RuntimeException exception) {
            ViewerLog.error("Error al propagar", exception);
            previewPanel.showSaveStatus(
                "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
            return false;
        }
    }

    /** Propagación desde la interfaz: el CLI no bloquea el hilo de JavaFX. */
    private void propagateAsync(EditorModel editor) {
        String content = editor.sharedBody();
        beginBusy("Calculando cambios…");
        Thread.ofVirtual().start(() -> {
            try {
                SaveData dryRun = cliClient.saveGroup(editor.path(), content, true, editor.project());
                Platform.runLater(() -> {
                    endBusy("Listo");
                    if (!dryRun.written()) {
                        editor.markSharedBodySaved();
                        previewPanel.showSaveStatus("Sin cambios que propagar.", false);
                        return;
                    }
                    boolean confirmed = DiffReviewDialog.confirm(stage, "Propagar cambios",
                        "Revisa los cambios antes de propagar al grupo.", DiffReview.summary(dryRun));
                    if (!confirmed) {
                        previewPanel.showSaveStatus("Propagación cancelada.", false);
                        return;
                    }
                    beginBusy("Propagando…");
                    Thread.ofVirtual().start(() -> executePropagate(editor, content));
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                ViewerLog.error("Error al calcular la propagación", exception);
                Platform.runLater(() -> {
                    previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
                    endBusy("Error al propagar");
                });
            } catch (RuntimeException exception) {
                ViewerLog.error("Error al calcular la propagación", exception);
                Platform.runLater(() -> {
                    previewPanel.showSaveStatus(
                        "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
                    endBusy("Error al propagar");
                });
            }
        });
    }

    private void executePropagate(EditorModel editor, String content) {
        try {
            SaveData result = cliClient.saveGroup(editor.path(), content, false, editor.project());
            Platform.runLater(() -> {
                editor.markSharedBodySaved();
                previewPanel.showSaveStatus("Propagado a " + result.members().size() + " miembros.", false);
                invalidateAfterWrite();
                refreshTree();
                endBusy("Propagado");
            });
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            ViewerLog.error("Error al propagar", exception);
            Platform.runLater(() -> {
                previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
                endBusy("Error al propagar");
            });
        } catch (RuntimeException exception) {
            ViewerLog.error("Error al propagar", exception);
            Platform.runLater(() -> {
                previewPanel.showSaveStatus(
                    "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
                endBusy("Error al propagar");
            });
        }
    }

    /** Vincula un artefacto legacy o desvinculado sin bloquear el hilo de JavaFX. */
    private void adoptAsync(EditorModel editor) {
        beginBusy("Calculando vinculación…");
        Thread.ofVirtual().start(() -> {
            try {
                AdoptData dryRun = cliClient.adopt(editor.path(), editor.project(), false);
                String body = "Grupo: " + dryRun.groupId();
                if (dryRun.diff() != null && !dryRun.diff().isBlank()) {
                    body += "\n\n" + dryRun.diff();
                } else {
                    body += "\n\nSin cambios de contenido; el artefacto se vinculará tal cual.";
                }
                String review = body;
                Platform.runLater(() -> {
                    endBusy("Listo");
                    boolean confirmed = DiffReviewDialog.confirm(stage, "Volver al grupo",
                        "Revisa el cambio antes de vincular el artefacto a su grupo.", review);
                    if (!confirmed) {
                        previewPanel.showSaveStatus("Adopción cancelada.", false);
                        return;
                    }
                    beginBusy("Vinculando…");
                    Thread.ofVirtual().start(() -> executeAdopt(editor));
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                ViewerLog.error("Error al calcular la adopción", exception);
                Platform.runLater(() -> {
                    previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
                    endBusy("Error al adoptar");
                });
            } catch (RuntimeException exception) {
                ViewerLog.error("Error al calcular la adopción", exception);
                Platform.runLater(() -> {
                    previewPanel.showSaveStatus(
                        "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
                    endBusy("Error al adoptar");
                });
            }
        });
    }

    private void executeAdopt(EditorModel editor) {
        try {
            cliClient.adopt(editor.path(), editor.project(), true);
            Platform.runLater(() -> {
                previewPanel.showSaveStatus("Vinculado al grupo.", false);
                invalidateAfterWrite();
                refreshTree();
                reloadSelectedArtifact();
                endBusy("Vinculado");
            });
        } catch (CliClientException exception) {
            ErrorNotice notice = ErrorPresenter.present(exception);
            ViewerLog.error("Error al adoptar", exception);
            Platform.runLater(() -> {
                previewPanel.showSaveStatus("Error: " + notice.message() + " " + notice.action(), true);
                endBusy("Error al adoptar");
            });
        } catch (RuntimeException exception) {
            ViewerLog.error("Error al adoptar", exception);
            Platform.runLater(() -> {
                previewPanel.showSaveStatus(
                    "Error: " + (exception.getMessage() == null ? "inesperado" : exception.getMessage()), true);
                endBusy("Error al adoptar");
            });
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
        previewPanel.showSaveStatus(outcome.ok()
            ? (outcome.message() == null ? "Guardado" : outcome.message())
            : "Error: " + outcome.message(), !outcome.ok());
    }

    private record SaveOutcome(boolean ok, String message, SavePart conflictPart) {
        static SaveOutcome success(String message) {
            return new SaveOutcome(true, message, null);
        }

        static SaveOutcome failure(String message) {
            return new SaveOutcome(false, message, null);
        }

        static SaveOutcome conflict(SavePart part, String message) {
            return new SaveOutcome(false, message, part);
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
                endBusy("Módulo creado");
                refreshAll();
                notifySuccess("Módulo creado",
                    "Se crearon " + result.created().size() + " artefactos.\n"
                        + "Se omitieron " + result.skipped().size() + ".");
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
        List<String> modules = projectSession.status().project().modules();
        if (modules.isEmpty()) {
            showErrorAlert("Sin módulos", null, "No hay módulos registrados en el proyecto.");
            return;
        }

        var result = AddTechnicianDialog.show(stage, modules, providerIds());
        if (result.isEmpty()) {
            return;
        }

        AddTechnicianDialog.Result params = result.get();
        String selectedModule = params.module();
        if (params.technicians().isEmpty() || params.providers().isEmpty() || selectedModule == null) {
            showErrorAlert("Faltan datos", null, "Selecciona módulo, técnicos y proveedores.");
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
                endBusy("Técnicos añadidos");
                refreshAll();
                notifySuccess("Técnicos añadidos",
                    "Se crearon " + result.created().size() + " artefactos.\n"
                        + "Se omitieron " + result.skipped().size() + ".");
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
        Path project = hasProject ? projectSession.path() : null;

        CreateArtifactWizard.CatalogLoader catalogLoader = (scope, selectedProviders, kinds) -> {
            Path scopedProject = "project".equals(scope) ? project : null;
            List<CreateArtifactWizard.CatalogItem> items = new ArrayList<>();
            for (String provider : selectedProviders) {
                for (String kind : kinds) {
                    CatalogData catalog = cliClient.catalog(provider, scope, kind, null, scopedProject);
                    for (CatalogData.CatalogEntry entry : catalog.entries()) {
                        items.add(new CreateArtifactWizard.CatalogItem(provider, scope, kind, entry.name(),
                            entry.relativePath(), entry.module(), entry.technician(), entry.exists()));
                    }
                }
            }
            return items;
        };

        CreateArtifactWizard.TemplateLoader templateLoader = item -> {
            Path scopedProject = "project".equals(item.scope()) ? project : null;
            CatalogData data = cliClient.catalog(item.provider(), item.scope(), item.kind(), item.name(),
                scopedProject);
            return data.entries().isEmpty() ? null : data.entries().getFirst().template();
        };

        CreateArtifactWizard.show(stage, providers, hasProject, catalogLoader, templateLoader)
            .ifPresent(this::applyArtifactCreation);
    }

    private void applyArtifactCreation(CreateArtifactWizard.Request request) {
        Path project = "project".equals(request.scope()) ? currentProject() : null;
        beginBusy("Creando artefactos…");
        Thread.ofVirtual().start(() -> {
            int created = 0;
            int replaced = 0;
            int skipped = 0;
            List<String> problems = new ArrayList<>();
            for (CreateArtifactWizard.CatalogItem item : request.items()) {
                try {
                    CreateArtifactData data = cliClient.createArtifact(item.provider(), item.scope(), item.kind(),
                        item.name(), project, request.contents().get(item.key()), request.replace());
                    if (data.replaced()) {
                        replaced++;
                    } else {
                        created++;
                    }
                } catch (CliClientException exception) {
                    if ("conflict.artifact.already_exists".equals(exception.code()) && !request.replace()) {
                        skipped++;
                    } else {
                        ErrorNotice notice = ErrorPresenter.present(exception);
                        problems.add(item.provider() + " · " + item.name() + ": " + notice.message());
                    }
                } catch (RuntimeException exception) {
                    problems.add(item.provider() + " · " + item.name() + ": " + unexpectedMessage(exception));
                }
            }

            int finalCreated = created;
            int finalReplaced = replaced;
            int finalSkipped = skipped;
            Platform.runLater(() -> {
                invalidateAfterWrite();
                refreshAll();
                StringBuilder message = new StringBuilder();
                message.append("Ámbito: ")
                    .append("project".equals(request.scope()) ? "proyecto" : "global")
                    .append('\n')
                    .append(counts(finalCreated, finalReplaced, finalSkipped));
                if (!problems.isEmpty()) {
                    message.append("\n\nIncidencias:\n").append(String.join("\n", problems));
                }
                message.append(request.replace()
                    ? "\n\nLos artefactos existentes se reemplazaron y tienen copia de seguridad."
                    : "\n\nLos artefactos existentes no se modificaron.");
                endBusy("Artefactos creados");
                if (problems.isEmpty()) {
                    notifySuccess("Artefactos creados", message.toString());
                } else {
                    notifyWarning("Artefactos creados con incidencias", message.toString());
                }
            });
        });
    }

    private static int size(List<String> values) {
        return values == null ? 0 : values.size();
    }

    private static String counts(int created, int replaced, int skipped) {
        List<String> parts = new ArrayList<>();
        parts.add(count(created, "creado"));
        if (replaced > 0) {
            parts.add(count(replaced, "reemplazado"));
        }
        if (skipped > 0) {
            parts.add(count(skipped, "omitido"));
        }
        return String.join(", ", parts);
    }

    private static String count(int value, String singular) {
        return value + " " + (value == 1 ? singular : singular + "s");
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
        if (cacheStore != null) {
            cacheStore.invalidateAll();
        }
        Path project = projectSession == null ? null : projectSession.path();
        if (project == null) {
            // Sin proyecto solo existe el ámbito global: se refrescan el árbol y el estado de proveedores.
            refreshTree();
            loadGlobalStatus();
            return;
        }
        try {
            StatusData status = projectSession.select(project);
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
        if (statusBar != null) {
            statusBar.setDirty(dirty);
        }
        if (dirty && previewPanel != null && previewPanel.currentPath() != null) {
            lastDraftPath = previewPanel.currentPath().toString();
            scheduleDraftSave();
        } else if (!dirty) {
            deleteDraft(lastDraftPath);
            lastDraftPath = null;
        }
        if (treeView != null) {
            treeView.refresh();
        }
        updateWindowTitle();
        updateMenuStates();
    }

    private void scheduleDraftSave() {
        if (draftSaveDelay == null || draftStore == null || previewPanel == null || !previewPanel.isDirty()) {
            return;
        }
        if (previewPanel.currentPath() != null) {
            lastDraftPath = previewPanel.currentPath().toString();
        }
        draftSaveDelay.playFromStart();
    }

    private void saveDraftNow() {
        if (draftStore == null || previewPanel == null || !previewPanel.isDirty()) {
            return;
        }
        if (previewPanel.isSkillMode()) {
            draftStore.save(previewPanel.skillEditor());
        } else {
            draftStore.save(previewPanel.editor());
        }
        refreshDraftStatus();
        ViewerLog.info("Borrador guardado para " + previewPanel.currentPath());
    }

    private void deleteDraft(String artifactPath) {
        if (draftStore == null || artifactPath == null || artifactPath.isBlank()) {
            return;
        }
        draftStore.delete(artifactPath);
        refreshDraftStatus();
    }

    private DraftStore.Draft findDraft(String artifactPath) {
        return draftStore == null ? null : draftStore.find(artifactPath).orElse(null);
    }

    private static void applyDraft(EditorModel editor, DraftStore.Draft draft) {
        if (draft.header() != null) {
            editor.setHeader(draft.header());
        }
        if (draft.body() != null) {
            editor.setBody(draft.body());
        }
        if (draft.sharedBody() != null) {
            editor.setSharedBody(draft.sharedBody());
        }
    }

    private void refreshDraftStatus() {
        int count = 0;
        if (draftStore != null) {
            count = draftStore.load().size();
        }
        if (statusBar != null) {
            statusBar.setDraftCount(count);
        }
        if (discardDraftsMenuItem != null) {
            discardDraftsMenuItem.setDisable(count == 0);
        }
    }

    private void discardDrafts() {
        if (draftStore == null) {
            return;
        }
        int count = draftStore.load().size();
        if (count == 0) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Descartar borradores");
        alert.setHeaderText("Se descartarán " + count + " borrador(es) sin guardar.");
        alert.setContentText("Esta acción no modifica los ficheros del proyecto.");
        Theme.apply(alert);
        ButtonType discard = new ButtonType("Descartar", ButtonBar.ButtonData.YES);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(discard, cancel);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == discard) {
            if (draftSaveDelay != null) {
                draftSaveDelay.stop();
            }
            draftStore.clear();
            lastDraftPath = null;
            refreshDraftStatus();
            setStatus("Borradores descartados");
        }
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
        if (statusBar == null) {
            return;
        }
        boolean running = projectWatcher != null && projectWatcher.isRunning();
        statusBar.setWatcher(running);
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
        if (statusBar != null && busyCount == 0) {
            statusBar.setMessage(message);
        }
    }

    private void beginBusy(String message) {
        busyCount++;
        if (statusBar != null) {
            statusBar.setBusy(true, message);
        }
    }

    private void endBusy(String message) {
        busyCount = Math.max(0, busyCount - 1);
        if (busyCount == 0 && statusBar != null) {
            statusBar.setBusy(false, message == null ? "Listo" : message);
        }
    }

    private void requestExit() {
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void closeResources() {
        if (draftSaveDelay != null) {
            draftSaveDelay.stop();
        }
        if (previewPanel != null && !previewPanel.isDirty()) {
            deleteDraft(lastDraftPath);
            lastDraftPath = null;
        }
        if (projectWatcher != null) {
            projectWatcher.close();
        }
        if (ptyConsolePanel != null) {
            ptyConsolePanel.shutdown();
        }
        ViewerLog.info("Visor cerrado");
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
            Ctrl+N        Aplicar artefactos
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
        String versions = versionStatus == null
            ? "Visor: " + ViewerVersion.current() + "\nCLI: comprobando…"
            : versionStatus.aboutText();
        alert.setContentText("""
            Visor de escritorio para el entorno agéntico de desarrollo.

            """ + versions + """

            JavaFX 21 · Consume el CLI mediante el contrato cli-json.
            Todo se ejecuta en tu equipo; no requiere conexión a internet.

            Log de diagnóstico:
            """ + ViewerLog.file());
        alert.showAndWait();
    }

    private void openLogFolder() {
        try {
            Path directory = AppPaths.stateDirectory();
            Files.createDirectories(directory);
            getHostServices().showDocument(directory.toUri().toString());
        } catch (IOException | RuntimeException exception) {
            ViewerLog.error("No se pudo abrir la carpeta de logs", exception);
            showInfoAlert("Carpeta de logs", "Ruta: " + ViewerLog.file());
        }
    }

    private void showErrorAlert(String title, String header, String message) {
        ViewerLog.warn("Error mostrado al usuario: " + title + " · " + message);
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

    /** Éxito no modal: toast en la esquina y mensaje en la barra de estado. */
    private void notifySuccess(String title, String message) {
        notify(title, message, false);
    }

    /** Resultado no modal con avisos que conviene destacar algo más de tiempo. */
    private void notifyWarning(String title, String message) {
        notify(title, message, true);
    }

    private void notify(String title, String message, boolean warning) {
        ViewerLog.info("Aviso (" + (warning ? "warning" : "ok") + "): " + title
            + (message == null || message.isBlank() ? "" : " · " + message));
        if (toastHost != null) {
            toastHost.show(title, message, warning);
        }
        if (statusBar != null) {
            statusBar.setMessage(title);
        }
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
        text.append(TreePresentation.kindLabel(node.type())).append(" · ").append(node.name());
        if (TreeGroups.GROUP.equals(node.type())) {
            int count = node.children() == null ? 0 : node.children().size();
            text.append('\n').append(count).append(count == 1 ? " elemento" : " elementos");
            return text.toString();
        }
        if (node.provider() != null) {
            text.append("\nProveedor: ").append(node.provider());
        }
        if (node.status() != null) {
            text.append(" · Estado: ").append(TreePresentation.statusLabel(node.status()));
        }
        if (node.pending()) {
            text.append(" · Pendiente");
        }
        if (node.path() != null) {
            text.append('\n').append(node.path());
        }
        return text.toString();
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
            ViewerLog.error("Error no controlado en " + thread.getName(), error));
        launch(args);
    }
}
