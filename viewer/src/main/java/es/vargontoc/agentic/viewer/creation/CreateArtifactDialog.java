package es.vargontoc.agentic.viewer.creation;

import java.util.List;
import java.util.Optional;

import es.vargontoc.agentic.viewer.ui.Icons;
import es.vargontoc.agentic.viewer.ui.Theme;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * Diálogo para seleccionar proveedor, ámbito y tipo al crear un artefacto. El
 * nombre se elige después, a partir del catálogo del proveedor.
 */
public final class CreateArtifactDialog {

    private CreateArtifactDialog() {}

    public record Result(String provider, String scope, String kind) {}

    /**
     * Muestra el diálogo de selección inicial.
     *
     * @param owner       ventana propietaria
     * @param providers   lista de proveedores disponibles
     * @param hasProject  true si hay un proyecto configurado (para permitir ámbito "project")
     * @return resultado con proveedor, ámbito y tipo; vacío si se cancela
     */
    public static Optional<Result> show(Window owner, List<String> providers, boolean hasProject) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Crear artefacto");
        dialog.setHeaderText("Elige el proveedor, el ámbito y el tipo del artefacto.");
        Theme.apply(dialog);

        ButtonType next = new ButtonType("Siguiente", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(next, cancel);
        var nextButton = dialog.getDialogPane().lookupButton(next);
        nextButton.setDisable(providers.isEmpty());
        if (nextButton instanceof javafx.scene.control.Button button) {
            button.setGraphic(Icons.of(Icons.Icon.CHEVRON_RIGHT, 15, "button-icon"));
        }

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8));

        ChoiceBox<String> providerChoice = new ChoiceBox<>();
        providerChoice.getItems().addAll(providers);
        if (!providers.isEmpty()) {
            providerChoice.getSelectionModel().selectFirst();
        }

        ChoiceBox<String> scopeChoice = new ChoiceBox<>();
        scopeChoice.getItems().add("global");
        if (hasProject) {
            scopeChoice.getItems().add("project");
        }
        scopeChoice.getSelectionModel().selectFirst();

        ChoiceBox<String> kindChoice = new ChoiceBox<>();
        kindChoice.getItems().addAll("agent", "skill");
        kindChoice.getSelectionModel().selectFirst();

        grid.add(new Label("Proveedor"), 0, 0);
        grid.add(providerChoice, 1, 0);
        grid.add(new Label("Ámbito"), 0, 1);
        grid.add(scopeChoice, 1, 1);
        grid.add(new Label("Tipo"), 0, 2);
        grid.add(kindChoice, 1, 2);

        Label hint = new Label("El nombre y la plantilla se eligen en el paso siguiente.");
        hint.getStyleClass().add("muted");
        grid.add(hint, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != next) {
                return null;
            }
            String provider = providerChoice.getValue();
            String scope = scopeChoice.getValue();
            String kind = kindChoice.getValue();
            if (provider == null || scope == null || kind == null) {
                return null;
            }
            return new Result(provider, scope, kind);
        });

        return dialog.showAndWait();
    }
}
