package es.vargontoc.agentic.viewer.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import es.vargontoc.agentic.viewer.cli.model.TeamData;
import es.vargontoc.agentic.viewer.cli.model.TeamUpdateRequest;
import es.vargontoc.agentic.viewer.ui.Icons;
import es.vargontoc.agentic.viewer.ui.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Formulario de {@code .sdd/team.md}: permite editar módulos y las opciones
 * {@code sprints}, {@code git-policy}, {@code contract-strategy} y {@code readme-owner}
 * sin tocar el Markdown a mano. Devuelve el estado que se enviará a {@code team-update}.
 */
public final class TeamEditorDialog {

    private static final String NONE = "(ninguno)";

    private TeamEditorDialog() {}

    public static Optional<TeamUpdateRequest> show(Window owner, TeamData data) {
        Dialog<TeamUpdateRequest> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Equipo del proyecto");
        dialog.setHeaderText("Edita los módulos y las opciones de .sdd/team.md.\n"
            + "El CLI valida y guarda con copia de seguridad; el resto del fichero no se toca.");
        dialog.setResizable(true);
        Theme.apply(dialog);

        ButtonType save = new ButtonType("Guardar equipo", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(save, cancel);
        if (dialog.getDialogPane().lookupButton(save) instanceof Button button) {
            button.setGraphic(Icons.of(Icons.Icon.SAVE, 15, "button-icon"));
        }

        VBox content = new VBox(14);
        content.setPadding(new Insets(10));
        content.setPrefSize(780, 580);

        if (data.warnings() != null && !data.warnings().isEmpty()) {
            content.getChildren().add(warningBox(data.warnings()));
        }

        List<ModuleRow> rows = new ArrayList<>();
        VBox rowsBox = new VBox(6);
        rowsBox.getStyleClass().add("team-modules");

        ChoiceBox<String> sprints = choice(List.of("si", "no"), data.options().sprints());
        ChoiceBox<String> gitPolicy = choice(List.of("balanced", "strict"), data.options().gitPolicy());
        ChoiceBox<String> contractStrategy = choice(
            List.of("implementation-first", "contract-first"), data.options().contractStrategy());
        ChoiceBox<String> readmeOwner = new ChoiceBox<>();
        readmeOwner.setMaxWidth(Double.MAX_VALUE);

        Runnable refreshOwners = () -> {
            String previous = readmeOwner.getValue();
            List<String> names = new ArrayList<>();
            names.add(NONE);
            for (ModuleRow row : rows) {
                String name = row.name().getText() == null ? "" : row.name().getText().trim();
                if (!name.isEmpty() && !names.contains(name)) {
                    names.add(name);
                }
            }
            readmeOwner.getItems().setAll(names);
            if (previous != null && names.contains(previous)) {
                readmeOwner.setValue(previous);
            } else if (data.options().readmeOwner() != null && names.contains(data.options().readmeOwner())) {
                readmeOwner.setValue(data.options().readmeOwner());
            } else {
                readmeOwner.setValue(NONE);
            }
        };

        var saveButton = dialog.getDialogPane().lookupButton(save);
        Runnable validate = () -> {
            boolean anyBlank = rows.stream().anyMatch(row -> row.name().getText() == null
                || row.name().getText().trim().isEmpty());
            saveButton.setDisable(anyBlank || sprints.getValue() == null
                || gitPolicy.getValue() == null || contractStrategy.getValue() == null);
        };

        Consumer<TeamData.Module> addRow = module -> {
            ModuleRow row = new ModuleRow(module.name(), module.mapId(), String.join(" ", module.paths()), rowsBox);
            row.setOnRemove(() -> {
                rows.remove(row);
                refreshOwners.run();
                validate.run();
            });
            row.name().textProperty().addListener((o, previous, current) -> {
                refreshOwners.run();
                validate.run();
            });
            rows.add(row);
        };
        for (TeamData.Module module : data.modules()) {
            addRow.accept(module);
        }

        Button add = new Button("Añadir módulo");
        add.getStyleClass().add("secondary");
        add.setGraphic(Icons.of(Icons.Icon.PLUS, 14, "button-icon"));
        add.setOnAction(event -> {
            addRow.accept(new TeamData.Module("", null, List.of()));
            refreshOwners.run();
            validate.run();
        });

        Label modulesTitle = formLabel("Módulos");
        Label help = new Label("Identificador y rutas pueden quedar vacíos (se guardan como «-»). "
            + "Quitar un módulo no borra sus artefactos instalados.");
        help.getStyleClass().add("muted");
        help.setWrapText(true);

        ScrollPane moduleScroll = new ScrollPane(rowsBox);
        moduleScroll.setFitToWidth(true);
        moduleScroll.setPrefHeight(240);
        moduleScroll.getStyleClass().add("team-scroll");

        VBox modulesSection = new VBox(8, modulesTitle, help, moduleScroll, add);
        VBox optionsSection = new VBox(8,
            formLabel("Opciones"),
            optionRow("Sprints", sprints),
            optionRow("Git policy", gitPolicy),
            optionRow("Contract strategy", contractStrategy),
            optionRow("Readme owner", readmeOwner));

        content.getChildren().addAll(modulesSection, optionsSection);
        dialog.getDialogPane().setContent(content);

        refreshOwners.run();
        validate.run();

        dialog.setResultConverter(button -> {
            if (button != save) {
                return null;
            }
            List<TeamData.Module> modules = rows.stream()
                .map(row -> new TeamData.Module(row.name().getText().trim(),
                    blankToNull(row.mapId().getText()), splitPaths(row.paths().getText())))
                .toList();
            String readmeValue = readmeOwner.getValue();
            TeamData.Options options = new TeamData.Options(sprints.getValue(), gitPolicy.getValue(),
                contractStrategy.getValue(),
                readmeValue == null || NONE.equals(readmeValue) ? null : readmeValue.trim());
            return new TeamUpdateRequest(options, modules);
        });

        return dialog.showAndWait();
    }

    private static VBox warningBox(List<TeamData.Warning> warnings) {
        StringBuilder text = new StringBuilder("Avisos de la lectura actual:");
        for (TeamData.Warning warning : warnings) {
            text.append("\n· ").append(warning.type());
            if (warning.line() > 0) {
                text.append(" (línea ").append(warning.line()).append(')');
            }
        }
        Label label = new Label(text.toString());
        label.getStyleClass().add("warning");
        label.setWrapText(true);
        return new VBox(label);
    }

    private static HBox optionRow(String label, ChoiceBox<String> choice) {
        Label name = new Label(label);
        name.setMinWidth(140);
        HBox.setHgrow(choice, Priority.ALWAYS);
        choice.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(10, name, choice);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static ChoiceBox<String> choice(List<String> values, String selected) {
        ChoiceBox<String> choice = new ChoiceBox<>();
        choice.getItems().setAll(values);
        choice.setValue(values.contains(selected) ? selected : values.getFirst());
        choice.setMaxWidth(Double.MAX_VALUE);
        return choice;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return null;
        }
        return value.trim();
    }

    private static List<String> splitPaths(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.trim().split("\\s+"));
    }

    private static Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private static final class ModuleRow {
        private final TextField name = new TextField();
        private final TextField mapId = new TextField();
        private final TextField paths = new TextField();
        private final HBox node;
        private Runnable onRemove = () -> { };

        ModuleRow(String nameValue, String idValue, String pathsValue, VBox parent) {
            name.setText(nameValue == null ? "" : nameValue);
            mapId.setText(idValue == null ? "" : idValue);
            paths.setText(pathsValue == null ? "" : pathsValue);

            name.setPromptText("nombre");
            mapId.setPromptText("id (opcional)");
            paths.setPromptText("rutas separadas por espacios (opcional)");
            name.setPrefWidth(160);
            mapId.setPrefWidth(120);
            HBox.setHgrow(paths, Priority.ALWAYS);

            Button remove = new Button("Quitar");
            remove.getStyleClass().add("danger");
            node = new HBox(8, name, mapId, paths, remove);
            node.setAlignment(Pos.CENTER_LEFT);
            HBox row = node;
            remove.setOnAction(event -> {
                parent.getChildren().remove(row);
                onRemove.run();
            });
            parent.getChildren().add(node);
        }

        void setOnRemove(Runnable action) {
            this.onRemove = action == null ? () -> { } : action;
        }

        TextField name() {
            return name;
        }

        TextField mapId() {
            return mapId;
        }

        TextField paths() {
            return paths;
        }
    }
}
