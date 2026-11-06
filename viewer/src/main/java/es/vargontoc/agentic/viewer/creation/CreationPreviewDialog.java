package es.vargontoc.agentic.viewer.creation;

import java.util.Optional;

import es.vargontoc.agentic.viewer.cli.model.CreateModuleData;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Muestra el resultado de una simulación (dry-run) de create-module o add-technician
 * y pide confirmación antes de ejecutar el comando real.
 */
public final class CreationPreviewDialog {

    private CreationPreviewDialog() {}

    /**
     * @param title   título del diálogo
     * @param header  encabezado descriptivo
     * @param data    resultado de la simulación
     * @return true si el usuario confirma la ejecución real
     */
    public static boolean confirm(Window owner, String title, String header, CreateModuleData data) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType confirm = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, cancel);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        content.getChildren().add(new Label("Módulo: " + data.module()));

        TextArea preview = new TextArea(formatPreview(data));
        preview.setEditable(false);
        preview.setWrapText(false);
        preview.getStyleClass().add("console");
        preview.setPrefSize(600, 360);

        ScrollPane scroll = new ScrollPane(preview);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(360);
        content.getChildren().add(scroll);

        if (data.warnings() != null && !data.warnings().isEmpty()) {
            Label warnings = new Label("Avisos: " + String.join(", ", data.warnings()));
            warnings.getStyleClass().add("warning");
            warnings.setWrapText(true);
            content.getChildren().add(warnings);
        }

        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == confirm;
    }

    private static String formatPreview(CreateModuleData data) {
        StringBuilder sb = new StringBuilder();
        if (data.created() != null && !data.created().isEmpty()) {
            sb.append("Se creará:\n");
            for (String item : data.created()) {
                sb.append("  + ").append(item).append('\n');
            }
        } else {
            sb.append("Se creará: (nada)\n");
        }
        if (data.skipped() != null && !data.skipped().isEmpty()) {
            sb.append("\nSe omitirá (ya existe):\n");
            for (String item : data.skipped()) {
                sb.append("  - ").append(item).append('\n');
            }
        }
        return sb.toString();
    }
}
