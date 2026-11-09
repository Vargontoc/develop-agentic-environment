package es.vargontoc.agentic.viewer.creation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import es.vargontoc.agentic.viewer.ui.Theme;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Diálogo para añadir técnicos a un módulo existente: solo pide técnicos y proveedores.
 */
public final class AddTechnicianDialog {

    private static final List<String> TECHNICIANS = List.of("analista", "desarrollador", "documentador");

    private AddTechnicianDialog() {}

    public record Result(List<String> technicians, List<String> providers) {}

    /**
     * Muestra el diálogo.
     *
     * @param owner       ventana propietaria
     * @param moduleName  nombre del módulo al que se añaden técnicos
     * @param providerIds identificadores de proveedores disponibles (del status)
     * @return resultado con técnicos y proveedores seleccionados; vacío si se cancela
     */
    public static Optional<Result> show(Window owner, String moduleName, List<String> providerIds) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Añadir técnicos a " + moduleName);
        dialog.setHeaderText("Genera los agentes de nuevos técnicos en el módulo «" + moduleName + "».");
        Theme.apply(dialog);

        ButtonType add = new ButtonType("Añadir técnicos", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(add, cancel);

        VBox content = new VBox(12);
        content.setPadding(new Insets(8));
        content.setPrefWidth(380);

        Label techLabel = formLabel("Técnicos a añadir");
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

        content.getChildren().addAll(techLabel, techBox, provLabel, provBox);
        dialog.getDialogPane().setContent(content);

        var addButton = dialog.getDialogPane().lookupButton(add);
        Runnable validate = () -> addButton.setDisable(
            techChecks.values().stream().noneMatch(CheckBox::isSelected)
                || provChecks.values().stream().noneMatch(CheckBox::isSelected));
        techChecks.values().forEach(check -> check.selectedProperty().addListener((o, p, c) -> validate.run()));
        provChecks.values().forEach(check -> check.selectedProperty().addListener((o, p, c) -> validate.run()));
        validate.run();

        dialog.setResultConverter(buttonType -> {
            if (buttonType != add) {
                return null;
            }
            return new Result(selected(techChecks), selected(provChecks));
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
}
