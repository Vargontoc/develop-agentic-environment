package es.vargontoc.agentic.viewer.creation;

import java.util.Optional;

import es.vargontoc.agentic.viewer.cli.model.CatalogData;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
        dialog.setHeaderText("Edita la plantilla del artefacto «" + name + "» antes de crearlo.");

        ButtonType create = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(create, cancel);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Contenido editable
        TextArea textArea = new TextArea();
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(60);

        // Para agentes: mostrar body (editable) o content (si body es null)
        // Para skills: mostrar el contenido del primer archivo (generalmente SKILL.md)
        String initialContent = "";
        if (template != null) {
            if (template.body() != null) {
                initialContent = template.body();
            } else if (template.content() != null) {
                initialContent = template.content();
            } else if (template.files() != null && !template.files().isEmpty()) {
                // Para skills, mostrar el contenido del primer archivo
                CatalogData.TemplateFile firstFile = template.files().getFirst();
                if (firstFile.content() != null) {
                    initialContent = firstFile.content();
                }
            }
        }
        textArea.setText(initialContent);

        content.getChildren().add(new Label("Contenido:"));
        content.getChildren().add(textArea);

        // Advertencia si ya existe
        if (exists) {
            Label warning = new Label("⚠ Ya existe un artefacto con este nombre. Se sobrescribirá.");
            warning.setStyle("-fx-text-fill: orange;");
            content.getChildren().add(warning);
        }

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == create) {
                return new Result(textArea.getText(), exists);
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
