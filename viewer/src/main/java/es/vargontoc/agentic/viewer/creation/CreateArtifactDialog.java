package es.vargontoc.agentic.viewer.creation;

import java.util.List;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * Diálogo para seleccionar proveedor, ámbito, tipo y nombre al crear un artefacto.
 */
public final class CreateArtifactDialog {

    private CreateArtifactDialog() {}

    public record Result(String provider, String scope, String kind, String name) {}

    /**
     * Muestra el diálogo de selección inicial.
     *
     * @param owner       ventana propietaria
     * @param providers   lista de proveedores disponibles
     * @param hasProject  true si hay un proyecto configurado (para permitir ámbito "project")
     * @return resultado con proveedor, ámbito, tipo y nombre; vacío si se cancela
     */
    public static Optional<Result> show(Window owner, List<String> providers, boolean hasProject) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Crear artefacto");
        dialog.setHeaderText("Selecciona proveedor, ámbito, tipo y nombre del artefacto.");

        ButtonType next = new ButtonType("Siguiente", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(next, cancel);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Proveedor
        ChoiceBox<String> providerChoice = new ChoiceBox<>();
        providerChoice.getItems().addAll(providers);
        if (!providers.isEmpty()) {
            providerChoice.getSelectionModel().selectFirst();
        }

        // Ámbito
        ChoiceBox<String> scopeChoice = new ChoiceBox<>();
        scopeChoice.getItems().add("global");
        if (hasProject) {
            scopeChoice.getItems().add("project");
        }
        scopeChoice.getSelectionModel().selectFirst();

        // Tipo
        ChoiceBox<String> kindChoice = new ChoiceBox<>();
        kindChoice.getItems().addAll("agent", "skill");
        kindChoice.getSelectionModel().selectFirst();

        // Nombre (se seleccionará después de obtener el catálogo)
        ChoiceBox<String> nameChoice = new ChoiceBox<>();
        nameChoice.setDisable(true);

        grid.add(new Label("Proveedor:"), 0, 0);
        grid.add(providerChoice, 1, 0);
        grid.add(new Label("Ámbito:"), 0, 1);
        grid.add(scopeChoice, 1, 1);
        grid.add(new Label("Tipo:"), 0, 2);
        grid.add(kindChoice, 1, 2);
        grid.add(new Label("Nombre:"), 0, 3);
        grid.add(nameChoice, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == next) {
                String provider = providerChoice.getValue();
                String scope = scopeChoice.getValue();
                String kind = kindChoice.getValue();
                String name = nameChoice.getValue();
                if (provider != null && scope != null && kind != null && name != null) {
                    return new Result(provider, scope, kind, name);
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
