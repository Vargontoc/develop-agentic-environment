package es.vargontoc.agentic.viewer.creation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        dialog.setHeaderText("Registrar un módulo nuevo en .sdd/team.md y generar sus agentes y skills.");

        ButtonType create = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(create, cancel);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label nameLabel = new Label("Nombre del módulo:");
        TextField nameField = new TextField();
        nameField.setPromptText("Ej: auth, billing, notifications (mínimo 3 caracteres)");
        nameField.setPrefWidth(300);

        Label techLabel = new Label("Técnicos:");
        Map<String, CheckBox> techChecks = new LinkedHashMap<>();
        for (String tech : TECHNICIANS) {
            CheckBox cb = new CheckBox(tech);
            cb.setSelected(true);
            techChecks.put(tech, cb);
        }

        Label provLabel = new Label("Proveedores:");
        Map<String, CheckBox> provChecks = new LinkedHashMap<>();
        for (String prov : providerIds) {
            CheckBox cb = new CheckBox(prov);
            cb.setSelected(true);
            provChecks.put(prov, cb);
        }

        VBox techBox = new VBox(4);
        for (CheckBox cb : techChecks.values()) {
            techBox.getChildren().add(cb);
        }

        VBox provBox = new VBox(4);
        for (CheckBox cb : provChecks.values()) {
            provBox.getChildren().add(cb);
        }
        if (provChecks.isEmpty()) {
            provBox.getChildren().add(new Label("(sin proveedores configurados)"));
        }

        content.getChildren().addAll(nameLabel, nameField, techLabel, techBox, provLabel, provBox);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == create) {
                String name = nameField.getText().trim();
                List<String> technicians = new ArrayList<>();
                for (var entry : techChecks.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        technicians.add(entry.getKey());
                    }
                }
                List<String> providers = new ArrayList<>();
                for (var entry : provChecks.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        providers.add(entry.getKey());
                    }
                }
                return new Result(name, technicians, providers);
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
