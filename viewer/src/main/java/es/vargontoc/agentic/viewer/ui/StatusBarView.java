package es.vargontoc.agentic.viewer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Barra de estado inferior: progreso, mensajes, cambios sin guardar,
 * borradores locales, actualización automática y vigilancia de ficheros.
 */
public final class StatusBarView extends HBox {

    private final ProgressIndicator busy = new ProgressIndicator();
    private final Label message = new Label("Listo");
    private final Label dirty = new Label("Cambios sin guardar");
    private final Label draft = new Label();
    private final Label watcher = new Label("Watcher inactivo");
    private final CheckBox autoRefresh = new CheckBox("Auto-actualizar");
    private final CheckBox autoRefreshPreview = new CheckBox("Recargar vista");

    public StatusBarView(CheckMenuItem autoRefreshMenuItem) {
        getStyleClass().add("status-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPadding(new Insets(6, 14, 6, 14));

        busy.setPrefSize(14, 14);
        busy.setMinSize(14, 14);
        busy.setMaxSize(14, 14);
        busy.setManaged(false);
        busy.setVisible(false);

        message.getStyleClass().add("status-message");

        dirty.getStyleClass().add("warning");
        dirty.setGraphic(Icons.of(Icons.Icon.PENCIL, 12, "status-icon"));
        dirty.setManaged(false);
        dirty.setVisible(false);

        draft.getStyleClass().add("status-meta");
        draft.setGraphic(Icons.of(Icons.Icon.SAVE, 12, "status-icon"));
        draft.setManaged(false);
        draft.setVisible(false);
        draft.setTooltip(new Tooltip(
            "Copias locales de cambios sin guardar. Se recuperan al abrir el artefacto."));

        autoRefresh.setTooltip(new Tooltip("Refresca el árbol cuando cambian ficheros externos"));
        if (autoRefreshMenuItem != null) {
            autoRefresh.selectedProperty().bindBidirectional(autoRefreshMenuItem.selectedProperty());
        }

        autoRefreshPreview.setSelected(true);
        autoRefreshPreview.setTooltip(new Tooltip(
            "Si está marcado, también se recarga el artefacto abierto. Si no, solo se refresca el árbol."));

        watcher.getStyleClass().add("status-meta");
        watcher.setGraphic(Icons.of(Icons.Icon.REFRESH, 12, "status-icon"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().setAll(busy, message, dirty, draft, spacer, autoRefresh, autoRefreshPreview, watcher);
    }

    public void setBusy(boolean value, String text) {
        busy.setManaged(value);
        busy.setVisible(value);
        message.setText(text == null ? "Listo" : text);
    }

    public void setMessage(String text) {
        message.setText(text == null ? "Listo" : text);
    }

    public void setDirty(boolean value) {
        dirty.setText(value ? "● Cambios sin guardar" : "");
        dirty.setManaged(value);
        dirty.setVisible(value);
    }

    public void setDraftCount(int count) {
        draft.setText(count == 0 ? "" : count + (count == 1 ? " borrador" : " borradores"));
        draft.setManaged(count > 0);
        draft.setVisible(count > 0);
    }

    public void setWatcher(boolean running) {
        watcher.setText(running ? "Vigilando cambios" : "Watcher inactivo");
        watcher.getStyleClass().removeAll("ok", "muted");
        watcher.getStyleClass().add(running ? "ok" : "muted");
    }

    public boolean isAutoRefreshSelected() {
        return autoRefresh.isSelected();
    }

    public boolean isAutoRefreshPreviewSelected() {
        return autoRefreshPreview.isSelected();
    }
}
