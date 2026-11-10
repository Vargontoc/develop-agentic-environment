package es.vargontoc.agentic.viewer.creation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import es.vargontoc.agentic.viewer.ui.Icons;
import es.vargontoc.agentic.viewer.ui.Theme;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Diálogo para aplicar los artefactos de uno o varios proveedores en un ámbito.
 * Solo pide proveedores y ámbito: el catálogo relacionado de cada proveedor se
 * aplica después, omitiendo lo que ya exista.
 */
public final class CreateArtifactDialog {

    private CreateArtifactDialog() {}

    public record Result(List<String> providers, String scope) {}

    private record Scope(String id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Muestra el diálogo de selección.
     *
     * @param owner       ventana propietaria
     * @param providers   lista de proveedores disponibles
     * @param hasProject  true si hay un proyecto configurado (para permitir ámbito "project")
     * @return resultado con proveedores y ámbito; vacío si se cancela
     */
    public static Optional<Result> show(Window owner, List<String> providers, boolean hasProject) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Aplicar artefactos");
        dialog.setHeaderText("Elige los proveedores y el ámbito. Se aplicarán los artefactos de su catálogo.");
        Theme.apply(dialog);

        ButtonType apply = new ButtonType("Aplicar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(apply, cancel);

        VBox content = new VBox(12);
        content.setPadding(new Insets(8));
        content.setPrefWidth(420);

        Label providerLabel = formLabel("Proveedores");
        Map<String, CheckBox> providerChecks = new LinkedHashMap<>();
        for (String provider : providers) {
            CheckBox check = new CheckBox(provider);
            check.setSelected(true);
            providerChecks.put(provider, check);
        }

        VBox providerBox = new VBox(6);
        providerBox.getChildren().addAll(providerChecks.values());
        if (providerChecks.isEmpty()) {
            Label empty = new Label("No hay proveedores configurados.");
            empty.getStyleClass().add("warning");
            providerBox.getChildren().add(empty);
        }

        Label scopeLabel = formLabel("Ámbito");
        ChoiceBox<Scope> scopeChoice = new ChoiceBox<>();
        scopeChoice.getItems().add(new Scope("global", "Global"));
        if (hasProject) {
            scopeChoice.getItems().add(new Scope("project", "Proyecto actual"));
        }
        scopeChoice.getSelectionModel().selectFirst();
        scopeChoice.setMaxWidth(Double.MAX_VALUE);

        Label hint = new Label("Se crearán los artefactos que falten. Lo que ya exista no se modifica.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);

        content.getChildren().addAll(providerLabel, providerBox, scopeLabel, scopeChoice, hint);
        dialog.getDialogPane().setContent(content);

        var applyButton = dialog.getDialogPane().lookupButton(apply);
        if (applyButton instanceof javafx.scene.control.Button button) {
            button.setGraphic(Icons.of(Icons.Icon.CHECK, 15, "button-icon"));
        }
        Runnable validate = () -> applyButton.setDisable(
            providerChecks.values().stream().noneMatch(CheckBox::isSelected));
        providerChecks.values().forEach(check -> check.selectedProperty().addListener((o, p, c) -> validate.run()));
        validate.run();

        dialog.setResultConverter(buttonType -> {
            if (buttonType != apply) {
                return null;
            }
            Scope scope = scopeChoice.getValue();
            return scope == null ? null : new Result(selected(providerChecks), scope.id());
        });

        return dialog.showAndWait();
    }

    private static List<String> selected(Map<String, CheckBox> checks) {
        List<String> result = new ArrayList<>();
        for (var entry : checks.entrySet()) {
            if (entry.getValue().isSelected()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }
}
