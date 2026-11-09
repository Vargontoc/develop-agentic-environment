package es.vargontoc.agentic.viewer.diff;

import java.util.Optional;

import es.vargontoc.agentic.viewer.ui.Theme;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Diálogo de confirmación de propagación: muestra el diff calculado y pide
 * confirmar antes de escribir. Devuelve {@code true} si se confirma.
 */
public final class DiffReviewDialog {

    private DiffReviewDialog() {}

    public static boolean confirm(Window owner, String title, String header, String body) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setResizable(true);
        Theme.apply(dialog);

        ButtonType confirm = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, cancel);

        VBox content = new VBox(8);
        content.setPadding(new Insets(8));
        content.setPrefSize(720, 480);

        Label label = new Label("Cambios previstos");
        label.getStyleClass().add("form-label");

        TextArea text = new TextArea(body);
        text.setEditable(false);
        text.setWrapText(false);
        text.getStyleClass().add("console");
        VBox.setVgrow(text, Priority.ALWAYS);

        content.getChildren().addAll(label, text);
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == confirm;
    }
}
