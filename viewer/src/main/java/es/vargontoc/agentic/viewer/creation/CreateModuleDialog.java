package es.vargontoc.agentic.viewer.creation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import es.vargontoc.agentic.viewer.ui.Theme;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Diálogo para crear un módulo nuevo: pide nombre, técnicos y proveedores.
 */
public final class CreateModuleDialog {

    private static final List<String> TECHNICIANS = List.of("analista", "desarrollador", "documentador");

    private CreateModuleDialog() {}

    public record Result(String name, List<String> technicians, List<String> providers) {}

    /**
     * Muestra el diálogo.
     *
     * @param owner       ventana propietaria
     * @param providerIds identificadores de proveedores disponibles (del status)
     * @return resultado con nombre, técnicos y proveedores seleccionados; vacío si se cancela
     */
    public static Optional<Result> show(Window owner, List<String> providerIds) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Crear módulo");
        dialog.setHeaderText("Registra un módulo en .sdd/team.md y genera sus agentes y skills.");
        Theme.apply(dialog);

        ButtonType create = new ButtonType("Crear módulo", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(create, cancel);

        VBox content = new VBox(12);
        content.setPadding(new Insets(8));
        content.setPrefWidth(420);

        Label nameLabel = formLabel("Nombre del módulo");
        TextField nameField = new TextField();
        nameField.setPromptText("Ej.: auth, billing, notifications (mínimo 3 caracteres)");
        nameField.setMaxWidth(Double.MAX_VALUE);

        Label techLabel = formLabel("Técnicos");
        Map<String, CheckBox> techChecks = new LinkedHashMap<>();
        for (String tech : TECHNICIANS) {
            CheckBox cb = new CheckBox(tech);
            cb.setSelected(true);
            techChecks.put(tech, cb);
        }

        Label provLabel = formLabel("Proveedores");
        Map<String, CheckBox> provChecks = new LinkedHashMap<>();
        for (String prov : providerIds) {
            CheckBox cb = new CheckBox(prov);
            cb.setSelected(true);
            provChecks.put(prov, cb);
        }

        VBox techBox = checkBoxGroup(techChecks);
        VBox provBox = checkBoxGroup(provChecks);
        if (provChecks.isEmpty()) {
            Label empty = new Label("No hay proveedores configurados.");
            empty.getStyleClass().add("warning");
            provBox.getChildren().add(empty);
        }

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        content.getChildren().addAll(nameLabel, nameField, techLabel, techBox, provLabel, provBox, errorLabel);
        dialog.getDialogPane().setContent(content);

        var createButton = dialog.getDialogPane().lookupButton(create);
        Runnable validate = () -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            boolean hasTechnicians = techChecks.values().stream().anyMatch(CheckBox::isSelected);
            boolean hasProviders = provChecks.values().stream().anyMatch(CheckBox::isSelected);
            boolean nameOk = name.length() >= 3;
            createButton.setDisable(!(nameOk && hasTechnicians && hasProviders));
            if (!name.isBlank() && !nameOk) {
                showError(errorLabel, "El nombre debe tener al menos 3 caracteres.");
            } else if (!hasTechnicians && !techChecks.isEmpty()) {
                showError(errorLabel, "Selecciona al menos un técnico.");
            } else if (!hasProviders && !provChecks.isEmpty()) {
                showError(errorLabel, "Selecciona al menos un proveedor.");
            } else {
                hide(errorLabel);
            }
        };
        nameField.textProperty().addListener((observable, previous, current) -> validate.run());
        techChecks.values().forEach(check -> check.selectedProperty().addListener((o, p, c) -> validate.run()));
        provChecks.values().forEach(check -> check.selectedProperty().addListener((o, p, c) -> validate.run()));
        validate.run();
        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != create) {
                return null;
            }
            String name = nameField.getText().trim();
            return new Result(name, selected(techChecks), selected(provChecks));
        });

        return dialog.showAndWait();
    }

    private static VBox checkBoxGroup(Map<String, CheckBox> checks) {
        VBox box = new VBox(6);
        for (CheckBox check : checks.values()) {
            box.getChildren().add(check);
        }
        return box;
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

    private static void showError(Label label, String text) {
        label.setText(text);
        label.setManaged(true);
        label.setVisible(true);
    }

    private static void hide(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
    }
}
