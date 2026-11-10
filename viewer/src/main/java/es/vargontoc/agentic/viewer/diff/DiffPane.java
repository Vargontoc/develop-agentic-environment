package es.vargontoc.agentic.viewer.diff;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Vista de solo lectura de un diff con líneas coloreadas. Mantiene el scroll
 * horizontal para no romper líneas largas.
 */
public final class DiffPane extends ScrollPane {

    public DiffPane(String text) {
        getStyleClass().add("diff-scroll");
        setFitToWidth(false);
        setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        VBox lines = new VBox();
        lines.getStyleClass().add("diff-view");
        for (String line : (text == null ? "" : text).split("\\R", -1)) {
            Label label = new Label(line.isEmpty() ? " " : line);
            label.getStyleClass().add(DiffHighlighter.LINE);
            label.getStyleClass().add(DiffHighlighter.styleClass(line));
            label.setWrapText(false);
            label.setMaxWidth(Double.MAX_VALUE);
            lines.getChildren().add(label);
        }
        setContent(lines);
    }
}
