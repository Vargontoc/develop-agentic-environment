package es.vargontoc.agentic.viewer.ui;

import java.util.Objects;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

/**
 * Punto único para aplicar la hoja de estilos de la aplicación. Las escenas y
 * los diálogos creados por JavaFX no heredan la hoja del escenario principal,
 * así que cada uno debe recibirla explícitamente.
 */
public final class Theme {

    private static final String STYLESHEET = Objects.requireNonNull(
        Theme.class.getResource("/styles.css"), "No se encontró /styles.css").toExternalForm();

    private Theme() {}

    public static String stylesheet() {
        return STYLESHEET;
    }

    public static void apply(Scene scene) {
        if (scene != null && !scene.getStylesheets().contains(STYLESHEET)) {
            scene.getStylesheets().add(STYLESHEET);
        }
    }

    public static void apply(DialogPane pane) {
        if (pane != null && !pane.getStylesheets().contains(STYLESHEET)) {
            pane.getStylesheets().add(STYLESHEET);
            if (!pane.getStyleClass().contains("app-dialog")) {
                pane.getStyleClass().add("app-dialog");
            }
        }
    }

    public static void apply(Dialog<?> dialog) {
        if (dialog != null) {
            apply(dialog.getDialogPane());
        }
    }

    public static void apply(Alert alert) {
        if (alert != null) {
            apply(alert.getDialogPane());
        }
    }
}
