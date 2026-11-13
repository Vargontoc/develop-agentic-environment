package es.vargontoc.agentic.viewer.creation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import es.vargontoc.agentic.viewer.cli.model.CatalogData;
import es.vargontoc.agentic.viewer.ui.Icons;
import es.vargontoc.agentic.viewer.ui.Theme;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Asistente único para crear artefactos desde el catálogo:
 * <ol>
 *   <li>ámbito, proveedor(es), tipo y reemplazo;</li>
 *   <li>catálogo con búsqueda y selección múltiple;</li>
 *   <li>vista previa editable de cada artefacto seleccionado.</li>
 * </ol>
 * No escribe nada: devuelve la petición que {@code App} ejecuta con {@code create-artifact}.
 */
public final class CreateArtifactWizard {

    public record CatalogItem(String provider, String scope, String kind, String name, String relativePath,
                              String module, String technician, boolean exists) {
        public String key() {
            return provider + "|" + scope + "|" + kind + "|" + name;
        }

        public String kindLabel() {
            return "agent".equals(kind) ? "Agente" : "Skill";
        }
    }

    public record Request(String scope, List<CatalogItem> items, Map<String, String> contents, boolean replace) {
        public Request {
            items = List.copyOf(items);
            contents = contents == null ? Map.of() : Map.copyOf(contents);
        }
    }

    @FunctionalInterface
    public interface CatalogLoader {
        List<CatalogItem> load(String scope, List<String> providers, List<String> kinds) throws Exception;
    }

    @FunctionalInterface
    public interface TemplateLoader {
        CatalogData.Template load(CatalogItem item) throws Exception;
    }

    private CreateArtifactWizard() {}

    public static Optional<Request> show(Window owner, List<String> providers, boolean hasProject,
                                         CatalogLoader catalogLoader, TemplateLoader templateLoader) {
        Dialog<Request> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Crear artefactos");
        dialog.setHeaderText("Elige el ámbito, busca en el catálogo y revisa la plantilla antes de crear.");
        dialog.setResizable(true);
        Theme.apply(dialog);

        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);

        VBox content = new VBox(12);
        content.setPadding(new Insets(12));
        content.setPrefSize(940, 640);

        HBox steps = new HBox(8);
        Label step1 = stepLabel("1 · Ámbito");
        Label step2 = stepLabel("2 · Catálogo");
        Label step3 = stepLabel("3 · Vista previa");
        steps.getChildren().addAll(step1, arrow(), step2, arrow(), step3);

        Label message = new Label();
        message.getStyleClass().add("error");
        message.setWrapText(true);
        message.setManaged(false);
        message.setVisible(false);

        StackPane pages = new StackPane();
        VBox.setVgrow(pages, Priority.ALWAYS);

        // --- Paso 1 ---
        ChoiceBox<ScopeOption> scopeChoice = new ChoiceBox<>();
        scopeChoice.getItems().add(new ScopeOption("global", "Global"));
        if (hasProject) {
            scopeChoice.getItems().add(new ScopeOption("project", "Proyecto actual"));
        }
        scopeChoice.getSelectionModel().selectFirst();
        scopeChoice.setMaxWidth(Double.MAX_VALUE);

        VBox providerBox = new VBox(6);
        Map<String, CheckBox> providerChecks = new java.util.LinkedHashMap<>();
        for (String provider : providers) {
            CheckBox check = new CheckBox(provider);
            check.setSelected(true);
            providerChecks.put(provider, check);
            providerBox.getChildren().add(check);
        }

        CheckBox agents = new CheckBox("Agentes");
        agents.setSelected(true);
        CheckBox skills = new CheckBox("Skills");
        skills.setSelected(true);
        CheckBox replace = new CheckBox("Reemplazar los artefactos existentes");
        replace.setSelected(false);

        VBox stepOne = new VBox(12,
            formLabel("Ámbito"), scopeChoice,
            formLabel("Proveedores"), providerBox,
            formLabel("Tipo"), new HBox(14, agents, skills),
            replace,
            muted("Se mostrarán los artefactos del catálogo que se pueden crear en ese ámbito."));

        // --- Paso 2 ---
        TextField search = new TextField();
        search.setPromptText("Buscar por nombre, proveedor, tipo o módulo…");
        ListView<Row> catalogList = new ListView<>();
        VBox.setVgrow(catalogList, Priority.ALWAYS);

        Label count = new Label("0 seleccionados");
        count.getStyleClass().add("muted");
        Button selectVisible = new Button("Seleccionar visibles");
        selectVisible.getStyleClass().add("secondary");
        Button clearSelection = new Button("Quitar selección");
        clearSelection.getStyleClass().add("secondary");
        Label loading = new Label("Cargando catálogo…");
        loading.getStyleClass().add("muted");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(22, 22);
        HBox loadingBox = new HBox(8, progress, loading);
        loadingBox.setAlignment(Pos.CENTER_LEFT);
        loadingBox.setVisible(false);
        loadingBox.setManaged(false);

        VBox stepTwo = new VBox(8, search, catalogList,
            new HBox(8, selectVisible, clearSelection, count), loadingBox,
            muted("Los artefactos existentes solo se pueden crear si activas «Reemplazar los artefactos existentes»."));

        // --- Paso 3 ---
        ListView<Row> selectedList = new ListView<>();
        selectedList.setPrefWidth(260);
        selectedList.setCellFactory(list -> new PlainRowCell());
        TextArea editor = new TextArea();
        editor.setWrapText(false);
        editor.getStyleClass().add("editor");
        VBox.setVgrow(editor, Priority.ALWAYS);
        Label previewStatus = new Label("Selecciona un artefacto para ver su plantilla.");
        previewStatus.getStyleClass().add("muted");
        previewStatus.setWrapText(true);
        Label fileInfo = new Label();
        fileInfo.getStyleClass().add("muted");
        fileInfo.setWrapText(true);
        Button reset = new Button("Restablecer plantilla");
        reset.getStyleClass().add("secondary");
        reset.setDisable(true);
        HBox previewHeader = new HBox(8, formLabel("Plantilla editable"), spacer(), reset);
        VBox previewBox = new VBox(6, previewHeader, previewStatus, fileInfo, editor);
        VBox.setVgrow(editor, Priority.ALWAYS);
        HBox preview = new HBox(12, new VBox(6, formLabel("Seleccionados"), selectedList), previewBox);
        HBox.setHgrow(previewBox, Priority.ALWAYS);
        VBox.setVgrow(preview, Priority.ALWAYS);
        Label summary = new Label();
        summary.getStyleClass().add("muted");
        summary.setWrapText(true);
        VBox stepThree = new VBox(8, summary, preview,
            muted("La edición se aplica al contenido principal: el cuerpo del agente o SKILL.md. "
                + "Los ficheros de apoyo de una skill se copian de la plantilla."));
        VBox.setVgrow(preview, Priority.ALWAYS);

        pages.getChildren().addAll(stepOne, stepTwo, stepThree);

        // --- Navegación ---
        Button back = new Button("Atrás");
        back.getStyleClass().add("secondary");
        Button next = new Button("Siguiente");
        next.getStyleClass().add("primary");
        Button create = new Button("Crear artefactos");
        create.getStyleClass().add("primary");
        create.setGraphic(Icons.of(Icons.Icon.PLUS, 14, "button-icon"));
        HBox navigation = new HBox(8, back, spacer(), next, create);
        navigation.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().addAll(steps, message, pages, navigation);
        dialog.getDialogPane().setContent(content);

        final int[] step = {1};
        ObservableList<Row> rows = FXCollections.observableArrayList();
        FilteredList<Row> filtered = new FilteredList<>(rows, row -> true);
        catalogList.setItems(filtered);

        Map<String, CatalogData.Template> templates = new HashMap<>();
        Map<String, String> edited = new HashMap<>();
        final Row[] previewed = {null};
        final boolean[] updatingEditor = {false};
        final int[] loadVersion = {0};

        Runnable updateCount = () -> {
            long selected = rows.stream().filter(row -> row.selected.get()).count();
            count.setText(selected + (selected == 1 ? " seleccionado" : " seleccionados"));
            create.setDisable(selected == 0);
        };

        replace.selectedProperty().addListener((o, previous, current) -> {
            if (!current) {
                rows.stream().filter(row -> row.item.exists()).forEach(row -> row.selected.set(false));
            }
            catalogList.refresh();
            updateCount.run();
        });

        catalogList.setCellFactory(list -> new RowCell(replace, updateCount));

        Runnable showStep = () -> {
            stepOne.setVisible(step[0] == 1);
            stepOne.setManaged(step[0] == 1);
            stepTwo.setVisible(step[0] == 2);
            stepTwo.setManaged(step[0] == 2);
            stepThree.setVisible(step[0] == 3);
            stepThree.setManaged(step[0] == 3);
            back.setDisable(step[0] == 1);
            next.setVisible(step[0] < 3);
            next.setManaged(step[0] < 3);
            create.setVisible(step[0] == 3);
            create.setManaged(step[0] == 3);
            styleStep(step1, step[0] == 1);
            styleStep(step2, step[0] == 2);
            styleStep(step3, step[0] == 3);
            if (step[0] == 3) {
                refreshSelected(rows, selectedList, summary);
                updateCount.run();
            }
        };

        Runnable error = () -> {
            message.setManaged(false);
            message.setVisible(false);
            message.setText("");
        };

        Runnable showError = () -> {
            message.setManaged(true);
            message.setVisible(true);
        };

        Runnable loadCatalog = () -> {
            error.run();
            List<String> selectedProviders = providerChecks.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .toList();
            List<String> kinds = new ArrayList<>();
            if (agents.isSelected()) {
                kinds.add("agent");
            }
            if (skills.isSelected()) {
                kinds.add("skill");
            }
            if (selectedProviders.isEmpty() || kinds.isEmpty()) {
                message.setText(selectedProviders.isEmpty()
                    ? "Selecciona al menos un proveedor." : "Selecciona al menos un tipo (agente o skill).");
                showError.run();
                return;
            }
            String scope = scopeChoice.getValue().id();
            int version = ++loadVersion[0];
            loadingBox.setVisible(true);
            loadingBox.setManaged(true);
            next.setDisable(true);
            catalogList.setDisable(true);
            Thread.ofVirtual().start(() -> {
                try {
                    List<CatalogItem> items = catalogLoader.load(scope, selectedProviders, kinds);
                    javafx.application.Platform.runLater(() -> {
                        if (version != loadVersion[0]) {
                            return;
                        }
                        rows.clear();
                        for (CatalogItem item : items) {
                            rows.add(new Row(item));
                        }
                        String needle = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
                        filtered.setPredicate(row -> needle.isEmpty() || row.searchText().contains(needle));
                        catalogList.setDisable(false);
                        loadingBox.setVisible(false);
                        loadingBox.setManaged(false);
                        next.setDisable(false);
                        updateCount.run();
                    });
                } catch (Exception exception) {
                    javafx.application.Platform.runLater(() -> {
                        if (version != loadVersion[0]) {
                            return;
                        }
                        loadingBox.setVisible(false);
                        loadingBox.setManaged(false);
                        catalogList.setDisable(false);
                        next.setDisable(false);
                        message.setText("No se pudo cargar el catálogo: " + messageOf(exception));
                        showError.run();
                    });
                }
            });
        };

        search.textProperty().addListener((o, previous, value) -> {
            String needle = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(row -> needle.isEmpty() || row.searchText().contains(needle));
        });

        selectVisible.setOnAction(event -> {
            for (Row row : filtered) {
                if (!row.item.exists() || replace.isSelected()) {
                    row.selected.set(true);
                }
            }
            catalogList.refresh();
            updateCount.run();
        });
        clearSelection.setOnAction(event -> {
            rows.forEach(row -> row.selected.set(false));
            catalogList.refresh();
            updateCount.run();
        });

        Runnable showPreview = () -> {
            Row previous = previewed[0];
            if (previous != null && !updatingEditor[0]) {
                String key = previous.item.key();
                if (editor.getText().equals(initialContent(templates.get(key)))) {
                    edited.remove(key);
                } else {
                    edited.put(key, editor.getText());
                }
            }
            Row row = selectedList.getSelectionModel().getSelectedItem();
            previewed[0] = row;
            if (row == null) {
                previewStatus.setText("Selecciona un artefacto para ver su plantilla.");
                editor.setText("");
                editor.setDisable(true);
                reset.setDisable(true);
                fileInfo.setText("");
                return;
            }
            CatalogData.Template cached = templates.get(row.item.key());
            if (cached != null) {
                showTemplate(row.item, cached, editor, previewStatus, fileInfo, reset, edited, updatingEditor);
                return;
            }
            previewStatus.setText("Cargando plantilla de «" + row.item.name() + "»…");
            editor.setDisable(true);
            reset.setDisable(true);
            fileInfo.setText("");
            Thread.ofVirtual().start(() -> {
                try {
                    CatalogData.Template template = templateLoader.load(row.item);
                    javafx.application.Platform.runLater(() -> {
                        templates.put(row.item.key(), template);
                        if (previewed[0] == row) {
                            showTemplate(row.item, template, editor, previewStatus, fileInfo, reset, edited,
                                updatingEditor);
                        }
                    });
                } catch (Exception exception) {
                    javafx.application.Platform.runLater(() -> {
                        if (previewed[0] == row) {
                            previewStatus.setText("No se pudo cargar la plantilla: " + messageOf(exception));
                            editor.setDisable(true);
                            reset.setDisable(true);
                        }
                    });
                }
            });
        };

        selectedList.getSelectionModel().selectedItemProperty()
            .addListener((o, previous, current) -> showPreview.run());
        editor.textProperty().addListener((o, previous, value) -> {
            Row row = previewed[0];
            if (row != null && !updatingEditor[0]) {
                if (value != null && value.equals(initialContent(templates.get(row.item.key())))) {
                    edited.remove(row.item.key());
                } else {
                    edited.put(row.item.key(), value == null ? "" : value);
                }
            }
        });
        reset.setOnAction(event -> {
            Row row = previewed[0];
            CatalogData.Template template = row == null ? null : templates.get(row.item.key());
            if (row != null && template != null) {
                updatingEditor[0] = true;
                editor.setText(initialContent(template));
                updatingEditor[0] = false;
                if (editor.getText().equals(initialContent(template))) {
                    edited.remove(row.item.key());
                } else {
                    edited.put(row.item.key(), editor.getText());
                }
            }
        });

        next.setOnAction(event -> {
            if (step[0] == 1) {
                boolean anyProvider = providerChecks.values().stream().anyMatch(CheckBox::isSelected);
                if (!anyProvider) {
                    message.setText("Selecciona al menos un proveedor.");
                    showError.run();
                    return;
                }
                if (!agents.isSelected() && !skills.isSelected()) {
                    message.setText("Selecciona al menos un tipo (agente o skill).");
                    showError.run();
                    return;
                }
                step[0] = 2;
                showStep.run();
                loadCatalog.run();
            } else if (step[0] == 2) {
                if (rows.stream().noneMatch(row -> row.selected.get())) {
                    message.setText("Selecciona al menos un artefacto del catálogo.");
                    showError.run();
                    return;
                }
                error.run();
                step[0] = 3;
                showStep.run();
                if (selectedList.getItems().isEmpty()) {
                    summary.setText("No hay artefactos seleccionados.");
                } else {
                    selectedList.getSelectionModel().selectFirst();
                }
            }
        });
        back.setOnAction(event -> {
            if (step[0] > 1) {
                step[0]--;
                error.run();
                showStep.run();
            }
        });
        create.setOnAction(event -> {
            List<Row> selected = rows.stream().filter(row -> row.selected.get()).toList();
            if (selected.isEmpty()) {
                message.setText("Selecciona al menos un artefacto.");
                showError.run();
                return;
            }
            Map<String, String> contents = new HashMap<>();
            for (Row row : selected) {
                String value = edited.get(row.item.key());
                if (value != null) {
                    contents.put(row.item.key(), value);
                }
            }
            List<CatalogItem> items = selected.stream().map(row -> row.item).toList();
            dialog.setResult(new Request(scopeChoice.getValue().id(), items, contents, replace.isSelected()));
            dialog.close();
        });

        dialog.setOnCloseRequest(event -> { /* el cierre normal devuelve vacío */ });
        showStep.run();
        return dialog.showAndWait();
    }

    private static void refreshSelected(List<Row> rows, ListView<Row> selectedList, Label summary) {
        List<Row> selected = rows.stream().filter(row -> row.selected.get()).toList();
        Row previous = selectedList.getSelectionModel().getSelectedItem();
        selectedList.getItems().setAll(selected);
        if (previous != null && selected.contains(previous)) {
            selectedList.getSelectionModel().select(previous);
        } else if (!selected.isEmpty()) {
            selectedList.getSelectionModel().selectFirst();
        }
        summary.setText(selected.isEmpty() ? "No hay artefactos seleccionados."
            : selected.size() + (selected.size() == 1
                ? " artefacto seleccionado. Revisa y edita su plantilla."
                : " artefactos seleccionados. Elige cada uno para revisar su plantilla."));
    }

    private static void showTemplate(CatalogItem item, CatalogData.Template template, TextArea editor,
                                     Label status, Label fileInfo, Button reset, Map<String, String> edited,
                                     boolean[] updatingEditor) {
        String key = item.key();
        String content = edited.getOrDefault(key, initialContent(template));
        status.setText("Plantilla de «" + item.name() + "» (" + item.kindLabel().toLowerCase(Locale.ROOT) + ").");
        StringBuilder files = new StringBuilder();
        if (template != null && template.files() != null && !template.files().isEmpty()) {
            files.append("Ficheros de apoyo: ");
            for (int i = 0; i < template.files().size(); i++) {
                if (i > 0) {
                    files.append(", ");
                }
                files.append(template.files().get(i).path());
            }
        }
        fileInfo.setText(files.toString());
        updatingEditor[0] = true;
        editor.setText(content);
        updatingEditor[0] = false;
        editor.setDisable(false);
        reset.setDisable(false);
        if (template == null) {
            status.setText("Esta entrada no tiene plantilla; se creará con el contenido por defecto del CLI.");
        }
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
            for (CatalogData.TemplateFile file : template.files()) {
                if ("SKILL.md".equals(file.path()) && file.content() != null) {
                    return file.content();
                }
            }
            CatalogData.TemplateFile first = template.files().getFirst();
            if (first.content() != null) {
                return first.content();
            }
        }
        return "";
    }

    private static String messageOf(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
            ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private static Label stepLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("step-badge");
        return label;
    }

    private static Region arrow() {
        Label arrow = new Label("→");
        arrow.getStyleClass().add("muted");
        return arrow;
    }

    private static Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private static Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        label.setWrapText(true);
        return label;
    }

    private static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    private static void styleStep(Label label, boolean active) {
        label.getStyleClass().removeAll("step-badge-active");
        if (active) {
            label.getStyleClass().add("step-badge-active");
        }
    }

    private record ScopeOption(String id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final class Row {
        private final CatalogItem item;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);

        private Row(CatalogItem item) {
            this.item = item;
        }

        private String searchText() {
            return (item.provider() + " " + item.kindLabel() + " " + item.name() + " "
                + (item.module() == null ? "" : item.module()) + " "
                + (item.relativePath() == null ? "" : item.relativePath())).toLowerCase(Locale.ROOT);
        }

        private String displayText() {
            StringBuilder text = new StringBuilder();
            text.append(item.provider()).append(" · ").append(item.kindLabel())
                .append(" · ").append(item.name());
            if (item.module() != null && !item.module().isBlank()) {
                text.append("  (módulo ").append(item.module()).append(')');
            }
            if (item.technician() != null && !item.technician().isBlank()) {
                text.append("  [").append(item.technician()).append(']');
            }
            if (item.exists()) {
                text.append("  · ya existe");
            }
            return text.toString();
        }
    }

    private static final class RowCell extends ListCell<Row> {
        private final CheckBox check = new CheckBox();
        private final Label text = new Label();
        private final HBox box = new HBox(8, check, text);
        private final CheckBox replace;
        private final Runnable onToggle;

        private RowCell(CheckBox replace, Runnable onToggle) {
            this.replace = replace;
            this.onToggle = onToggle == null ? () -> { } : onToggle;
            box.setAlignment(Pos.CENTER_LEFT);
            text.setWrapText(false);
            check.setOnAction(event -> {
                if (getItem() != null) {
                    getItem().selected.set(check.isSelected());
                    onToggle.run();
                }
            });
        }

        @Override
        protected void updateItem(Row row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setGraphic(null);
                return;
            }
            check.setSelected(row.selected.get());
            check.setDisable(row.item.exists() && !replace.isSelected());
            text.setText(row.displayText());
            setGraphic(box);
        }
    }

    private static final class PlainRowCell extends ListCell<Row> {
        private final Label text = new Label();

        private PlainRowCell() {
            text.setWrapText(false);
        }

        @Override
        protected void updateItem(Row row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setGraphic(null);
                text.setText("");
                return;
            }
            text.setText(row.displayText());
            setGraphic(text);
        }
    }
}
