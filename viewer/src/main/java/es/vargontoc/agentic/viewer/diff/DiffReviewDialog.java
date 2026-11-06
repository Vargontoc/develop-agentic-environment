package es.vargontoc.agentic.viewer.diff;

import java.util.Optional;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
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

        ButtonType propagate = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(propagate, cancel);

        TextArea text = new TextArea(body);
        text.setEditable(false);
        text.setWrapText(false);
        text.getStyleClass().add("console");
        text.setPrefSize(660, 420);
        dialog.getDialogPane().setContent(text);

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == propagate;
    }
}
