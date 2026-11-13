package es.vargontoc.agentic.viewer.diff;

import es.vargontoc.agentic.viewer.ui.Icons;
import es.vargontoc.agentic.viewer.ui.Theme;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Diálogo de conflicto por cambios externos: muestra la versión del usuario y la
 * del disco (lado a lado y diff) y permite conservar la propia, usar la del disco
 * o cancelar. La escritura sigue siendo responsabilidad del CLI.
 */
public final class ConflictResolutionDialog {

    public enum Choice { KEEP_MINE, USE_DISK, CANCEL }

    /**
     * @param artifactName nombre visible del artefacto (agente, skill o fichero)
     * @param part         parte afectada, por ejemplo «Cuerpo» o «SKILL.md»
     * @param mine         contenido actual del editor
     * @param disk         contenido actual en disco
     */
    public record Request(String artifactName, String part, String mine, String disk) {}

    private ConflictResolutionDialog() {}

    public static Choice show(Window owner, Request request) {
        Dialog<Choice> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Conflicto por cambio externo");
        dialog.setHeaderText(request.artifactName() + " · " + request.part()
            + "\nEl fichero cambió fuera de la aplicación. Elige qué versión conservar.");
        dialog.setResizable(true);
        Theme.apply(dialog);

        ButtonType keepMine = new ButtonType("Conservar mi versión", ButtonBar.ButtonData.OK_DONE);
        ButtonType useDisk = new ButtonType("Usar la versión del disco", ButtonBar.ButtonData.OTHER);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(keepMine, useDisk, cancel);
        if (dialog.getDialogPane().lookupButton(keepMine) instanceof javafx.scene.control.Button button) {
            button.setGraphic(Icons.of(Icons.Icon.SAVE, 15, "button-icon"));
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(8));
        content.setPrefSize(900, 520);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab sideBySide = new Tab("Lado a lado");
        sideBySide.setContent(sideBySide(request));
        Tab diff = new Tab("Diff");
        diff.setContent(diffPane(request));
        tabs.getTabs().addAll(sideBySide, diff);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Label hint = new Label("Revisa «Lado a lado» y «Diff» antes de elegir. "
            + "«Conservar mi versión» sobrescribe el fichero y deja una copia de seguridad. "
            + "«Usar la versión del disco» descarta tus cambios en esta parte.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);

        content.getChildren().addAll(tabs, hint);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {
            if (button == keepMine) {
                return Choice.KEEP_MINE;
            }
            if (button == useDisk) {
                return Choice.USE_DISK;
            }
            return Choice.CANCEL;
        });

        return dialog.showAndWait().orElse(Choice.CANCEL);
    }

    private static SplitPane sideBySide(Request request) {
        VBox mine = versionBox("Tu versión", request.mine());
        VBox disk = versionBox("Versión en disco", request.disk());
        SplitPane split = new SplitPane(disk, mine);
        split.setDividerPositions(0.5);
        return split;
    }

    private static VBox versionBox(String title, String text) {
        Label label = new Label(title);
        label.getStyleClass().add("form-label");
        TextArea area = new TextArea(text == null ? "" : text);
        area.setEditable(false);
        area.setWrapText(false);
        area.getStyleClass().add("editor");
        VBox.setVgrow(area, Priority.ALWAYS);
        VBox box = new VBox(6, label, area);
        box.setPadding(new Insets(8));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private static VBox diffPane(Request request) {
        Label label = new Label("− versión en disco  ·  + tu versión");
        label.getStyleClass().add("muted");
        DiffPane pane = new DiffPane(TextDiff.between(request.disk(), request.mine()));
        VBox.setVgrow(pane, Priority.ALWAYS);
        VBox box = new VBox(6, label, pane);
        box.setPadding(new Insets(8));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }
}
