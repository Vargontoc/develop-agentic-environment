package es.vargontoc.agentic.viewer.ui;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Pantalla de bienvenida que guía los primeros pasos: abrir un proyecto,
 * iniciarlo y crear el primer artefacto. Solo muestra acciones; la lógica vive
 * en {@code App}.
 */
public final class WelcomePane extends VBox {

    /** Acción sin parámetros que dispara un paso de la guía. */
    public interface Action {
        void run();
    }

    private final Button openButton = new Button("Abrir proyecto");
    private final Button initButton = new Button("Iniciar proyecto");
    private final Button createButton = new Button("Crear artefactos");
    private final Label cliStatus = new Label();
    private final Label projectStatus = new Label("Aún no has abierto ningún proyecto.");
    private final Label versionInfo = new Label();
    private final VBox recentSection = new VBox(4);

    private boolean cliAvailable;
    private boolean hasProject;
    private boolean configured;
    private boolean providersAvailable;

    private Action onOpenProject = () -> { };
    private Action onInitProject = () -> { };
    private Action onCreateArtifact = () -> { };
    private Consumer<Path> onOpenRecent = path -> { };

    public WelcomePane() {
        getStyleClass().add("welcome");
        setAlignment(Pos.CENTER);
        setPadding(new Insets(32));
        setSpacing(22);

        Node mark = Icons.of(Icons.Icon.AGENT, 44, "welcome-mark");

        Label title = new Label("Agentic Viewer");
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label(
            "Revisa, edita y mantén los agentes y skills de tu entorno agéntico. "
                + "Todo se ejecuta en tu equipo y consume el CLI mediante su contrato JSON.");
        subtitle.getStyleClass().add("welcome-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(720);

        openButton.getStyleClass().add("primary");
        initButton.getStyleClass().add("secondary");
        createButton.getStyleClass().add("secondary");
        openButton.setGraphic(Icons.of(Icons.Icon.FOLDER_OPEN, 15, "button-icon"));
        initButton.setGraphic(Icons.of(Icons.Icon.PLAY, 15, "button-icon"));
        createButton.setGraphic(Icons.of(Icons.Icon.PLUS, 15, "button-icon"));
        openButton.setMaxWidth(Double.MAX_VALUE);
        initButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setMaxWidth(Double.MAX_VALUE);
        openButton.setAccessibleText("Abrir un proyecto del equipo");
        initButton.setAccessibleText("Iniciar la estructura .sdd del proyecto");
        createButton.setAccessibleText("Crear agentes y skills desde el catálogo");

        HBox steps = new HBox(16,
            step("1", Icons.Icon.FOLDER_OPEN, "Elige tu proyecto",
                "Abre la carpeta donde trabajas y valida su estado con el CLI.", openButton),
            step("2", Icons.Icon.PLAY, "Prepáralo",
                "Crea la estructura .sdd y registra el equipo si aún no existe.", initButton),
            step("3", Icons.Icon.PLUS, "Crea artefactos",
                "Elige agentes o skills del catálogo y revisa su plantilla.", createButton));
        steps.setAlignment(Pos.CENTER);
        steps.setFillHeight(true);

        cliStatus.getStyleClass().add("error");
        cliStatus.setWrapText(true);
        cliStatus.setManaged(false);
        cliStatus.setVisible(false);

        projectStatus.getStyleClass().add("muted");
        projectStatus.setWrapText(true);
        projectStatus.setMaxWidth(720);

        versionInfo.getStyleClass().addAll("welcome-version", "muted");
        versionInfo.setWrapText(true);
        versionInfo.setMaxWidth(720);

        recentSection.getStyleClass().add("recent-projects");
        recentSection.setManaged(false);
        recentSection.setVisible(false);

        VBox card = new VBox(18, title, subtitle, steps, recentSection, projectStatus, cliStatus,
            versionInfo);
        card.getStyleClass().add("welcome-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(940);
        card.setPadding(new Insets(28));

        getChildren().addAll(mark, card);

        openButton.setOnAction(event -> onOpenProject.run());
        initButton.setOnAction(event -> onInitProject.run());
        createButton.setOnAction(event -> onCreateArtifact.run());

        refreshControls();
    }

    public void setOnOpenProject(Action action) {
        this.onOpenProject = action == null ? () -> { } : action;
    }

    public void setOnInitProject(Action action) {
        this.onInitProject = action == null ? () -> { } : action;
    }

    public void setOnCreateArtifact(Action action) {
        this.onCreateArtifact = action == null ? () -> { } : action;
    }

    public void setOnOpenRecent(Consumer<Path> action) {
        this.onOpenRecent = action == null ? path -> { } : action;
    }

    /** Muestra los proyectos recientes como enlaces; oculta la sección si no hay ninguno. */
    public void setRecentProjects(List<Path> projects) {
        recentSection.getChildren().clear();
        if (projects == null || projects.isEmpty()) {
            recentSection.setManaged(false);
            recentSection.setVisible(false);
            return;
        }

        Label title = new Label("Proyectos recientes");
        title.getStyleClass().add("form-label");
        recentSection.getChildren().add(title);

        int limit = Math.min(projects.size(), 5);
        for (Path project : projects.subList(0, limit)) {
            Hyperlink link = new Hyperlink(displayName(project));
            link.setTooltip(new Tooltip(project.toString()));
            link.setMaxWidth(Double.MAX_VALUE);
            link.setAlignment(Pos.CENTER_LEFT);
            link.setOnAction(event -> onOpenRecent.accept(project));
            recentSection.getChildren().add(link);
        }
        recentSection.setManaged(true);
        recentSection.setVisible(true);
    }

    private static String displayName(Path project) {
        Path name = project.getFileName();
        Path parent = project.getParent();
        if (name == null) {
            return project.toString();
        }
        return parent == null ? name.toString() : name + "  ·  " + parent;
    }

    /** Informa de si el CLI está disponible y, si no, por qué. */
    public void setCli(boolean available, String error) {
        this.cliAvailable = available;
        if (available) {
            cliStatus.setText("");
            cliStatus.setManaged(false);
            cliStatus.setVisible(false);
        } else {
            cliStatus.setText(error == null || error.isBlank()
                ? "No se encontró el CLI. Revisa la carpeta viewer/cli." : error);
            cliStatus.setManaged(true);
            cliStatus.setVisible(true);
        }
        refreshControls();
    }

    /** Muestra las versiones del visor y del CLI y resalta una incompatibilidad. */
    public void setVersionInfo(String text, boolean warning) {
        versionInfo.setText(text == null ? "" : text);
        versionInfo.getStyleClass().removeAll("muted", "warning");
        versionInfo.getStyleClass().add(warning ? "warning" : "muted");
    }

    /** Actualiza el estado del proyecto para habilitar el paso correspondiente. */
    public void setProject(Path path, boolean configured) {
        this.hasProject = path != null;
        this.configured = configured;
        if (path == null) {
            projectStatus.setText("Aún no has abierto ningún proyecto.");
        } else if (configured) {
            projectStatus.setText("Proyecto: " + path + " · configurado");
        } else {
            projectStatus.setText("Proyecto: " + path + " · pendiente de iniciar");
        }
        refreshControls();
    }

    /** Indica si hay al menos un proveedor configurado para crear artefactos. */
    public void setProvidersAvailable(boolean available) {
        this.providersAvailable = available;
        refreshControls();
    }

    private void refreshControls() {
        openButton.setDisable(!cliAvailable);
        initButton.setDisable(!cliAvailable || !hasProject || configured);
        initButton.setText(configured ? "Proyecto iniciado" : "Iniciar proyecto");
        createButton.setDisable(!cliAvailable || !providersAvailable);
        createButton.setTooltip(providersAvailable
            ? new javafx.scene.control.Tooltip("Crear agentes y skills desde el catálogo")
            : new javafx.scene.control.Tooltip("Configura al menos un proveedor para aplicar artefactos"));
    }

    private static VBox step(String number, Icons.Icon icon, String title, String text, Button action) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("step-number");

        Node iconNode = Icons.of(icon, 16, "step-icon");
        HBox top = new HBox(8, numberLabel, iconNode);
        top.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("step-title");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("step-text");
        textLabel.setWrapText(true);

        VBox box = new VBox(8, top, titleLabel, textLabel, action);
        box.getStyleClass().add("step-card");
        box.setPadding(new Insets(18));
        box.setPrefWidth(220);
        box.setMinWidth(180);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }
}
