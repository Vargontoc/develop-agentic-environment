package es.vargontoc.agentic.viewer.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Avisos no modales apilados en la esquina inferior derecha. No capturan el
 * ratón y desaparecen solos, para no interrumpir el flujo de trabajo.
 */
public final class ToastHost extends StackPane {

    private static final int MAX_VISIBLE = 3;

    private final VBox stack = new VBox(8);

    public ToastHost() {
        setMouseTransparent(true);
        setPickOnBounds(false);
        stack.setAlignment(Pos.BOTTOM_RIGHT);
        stack.setFillWidth(true);
        stack.setMaxWidth(420);
        stack.setPadding(new Insets(0, 16, 48, 0));
        StackPane.setAlignment(stack, Pos.BOTTOM_RIGHT);
        getChildren().add(stack);
    }

    public void show(String title, String message) {
        show(title, message, false);
    }

    public void show(String title, String message, boolean warning) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(title, message, warning));
            return;
        }

        VBox card = new VBox(4);
        card.getStyleClass().add("toast");
        if (warning) {
            card.getStyleClass().add("toast-warning");
        }
        card.setMaxWidth(400);

        Label titleLabel = new Label(title == null ? "Aviso" : title);
        titleLabel.getStyleClass().add("toast-title");
        String body = message == null ? "" : message;
        if (body.length() > 800) {
            body = body.substring(0, 800) + "…";
        }
        Label messageLabel = new Label(body);
        messageLabel.getStyleClass().add("toast-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);
        card.getChildren().addAll(titleLabel, messageLabel);

        stack.getChildren().add(card);
        while (stack.getChildren().size() > MAX_VISIBLE) {
            stack.getChildren().removeFirst();
        }

        FadeTransition in = new FadeTransition(Duration.millis(180), card);
        in.setFromValue(0);
        in.setToValue(1);
        PauseTransition wait = new PauseTransition(warning ? Duration.seconds(9) : Duration.seconds(6));
        FadeTransition out = new FadeTransition(Duration.millis(260), card);
        out.setFromValue(1);
        out.setToValue(0);

        SequentialTransition sequence = new SequentialTransition(in, wait, out);
        sequence.setOnFinished(event -> stack.getChildren().remove(card));
        sequence.play();
    }
}
