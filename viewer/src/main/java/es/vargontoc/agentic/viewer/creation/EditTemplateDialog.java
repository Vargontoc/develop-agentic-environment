package es.vargontoc.agentic.viewer.creation;

import java.util.Optional;

import es.vargontoc.agentic.viewer.cli.model.CatalogData;
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
 * Diálogo para editar la plantilla de un artefacto antes de crearlo.
 */
public final class EditTemplateDialog {

    private EditTemplateDialog() {}

    public record Result(String content, boolean overwrite) {}

    /**
     * Muestra el diálogo de edición de plantilla.
     *
     * @param owner    ventana propietaria
     * @param name     nombre del artefacto
     * @param template plantilla obtenida del catálogo
     * @param exists   true si ya existe un artefacto con ese nombre
     * @return resultado con contenido editado y bandera de sobrescritura; vacío si se cancela
     */
    public static Optional<Result> show(Window owner, String name, CatalogData.Template template, boolean exists) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Editar plantilla");
        dialog.setHeaderText("Revisa la plantilla de «" + name + "» antes de crearla.");
        dialog.setResizable(true);
        Theme.apply(dialog);

        ButtonType create = new ButtonType(exists ? "Sobrescribir" : "Crear", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(create, cancel);

        VBox content = new VBox(10);
        content.setPadding(new Insets(8));
        content.setPrefSize(760, 520);

        Label label = new Label("Contenido");
        label.getStyleClass().add("form-label");

        TextArea textArea = new TextArea();
        textArea.setPrefRowCount(20);
        textArea.setWrapText(false);
        textArea.getStyleClass().add("editor");
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setText(initialContent(template));

        content.getChildren().addAll(label, textArea);

        if (exists) {
            Label warning = new Label("Ya existe un artefacto con este nombre. Al continuar se sobrescribirá.");
            warning.getStyleClass().add("warning");
            warning.setWrapText(true);
            content.getChildren().add(warning);
        }

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> buttonType == create
            ? new Result(textArea.getText(), exists)
            : null);

        return dialog.showAndWait();
    }

    private static String initialContent(CatalogData.Template template) {
        if (template == null) {
            return "";
        }
        if (template.body() != null) {
            return template.body();
        }
        if (template.content() != null) {
            return template.content();
        }
        if (template.files() != null && !template.files().isEmpty()) {
            CatalogData.TemplateFile firstFile = template.files().getFirst();
            if (firstFile.content() != null) {
                return firstFile.content();
            }
        }
        return "";
    }
}
