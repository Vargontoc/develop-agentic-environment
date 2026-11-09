package es.vargontoc.agentic.viewer.terminal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import es.vargontoc.agentic.viewer.cli.CliCommand;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Panel de consola PTY que ejecuta el CLI interactivo. Muestra la salida en
 * un área de texto, permite enviar líneas, interrumpir, cerrar y reiniciar la
 * sesión.
 */
public final class PtyConsolePanel extends VBox {

    private final CliCommand cliCommand;
    private final Supplier<Path> workingDirectory;

    private final TextArea output = new TextArea();
    private final TextField input = new TextField();
    private final Button startButton = new Button("Iniciar");
    private final Button sendButton = new Button("Enviar");
    private final Button interruptButton = new Button("Interrumpir");
    private final Button restartButton = new Button("Reiniciar");
    private final Button closeButton = new Button("Cerrar");
    private final Button clearButton = new Button("Limpiar");
    private final Label statusDot = new Label("●");
    private final Label statusLabel = new Label("Consola cerrada");

    private PtySession session;
    private Thread readerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Consumer<Boolean> onStateChanged = active -> { };

    /** Proveedor funcional para resolver el directorio de trabajo. */
    public interface Supplier<T> {
        T get();
    }

    public PtyConsolePanel(CliCommand cliCommand, Supplier<Path> workingDirectory) {
        this.cliCommand = cliCommand;
        this.workingDirectory = workingDirectory;
        setupUI();
        updateButtonStates();
    }

    private void setupUI() {
        getStyleClass().add("pty-console");
        setPadding(new Insets(14));
        setSpacing(10);

        startButton.getStyleClass().add("primary");
        sendButton.getStyleClass().add("primary");
        interruptButton.getStyleClass().add("danger");
        restartButton.getStyleClass().add("secondary");
        closeButton.getStyleClass().add("secondary");
        clearButton.getStyleClass().add("ghost");

        startButton.setTooltip(new Tooltip("Inicia el CLI interactivo en el proyecto actual"));
        interruptButton.setTooltip(new Tooltip("Envía Ctrl+C al proceso"));
        restartButton.setTooltip(new Tooltip("Cierra y vuelve a iniciar la sesión"));
        clearButton.setTooltip(new Tooltip("Limpia la salida de la consola"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusDot.getStyleClass().add("status-dot");
        statusLabel.getStyleClass().add("muted");

        HBox toolbar = new HBox(8, startButton, interruptButton, restartButton, closeButton, clearButton,
            spacer, statusDot, statusLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        output.setEditable(false);
        output.setWrapText(false);
        output.getStyleClass().add("console");
        output.setPrefRowCount(14);
        output.setText("La consola está cerrada.\nPulsa «Iniciar» para ejecutar el CLI interactivo del proyecto.\n");
        VBox.setVgrow(output, Priority.ALWAYS);

        input.setPromptText("Escribe una opción del menú o un comando y pulsa Enter…");
        input.setOnAction(event -> sendLine());
        input.setDisable(true);

        sendButton.setOnAction(event -> sendLine());
        interruptButton.setOnAction(event -> sendInterrupt());
        restartButton.setOnAction(event -> restart());
        closeButton.setOnAction(event -> close());
        clearButton.setOnAction(event -> clearOutput());
        startButton.setOnAction(event -> startSession());

        HBox inputBar = new HBox(8, input, sendButton);
        HBox.setHgrow(input, Priority.ALWAYS);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(toolbar, output, inputBar);
    }

    /** Registra un callback que se invoca cuando la consola pasa a activa o se cierra. */
    public void setOnStateChanged(Consumer<Boolean> listener) {
        this.onStateChanged = listener == null ? active -> { } : listener;
    }

    /** Inicia la sesión PTY con el CLI interactivo. */
    public void startSession() {
        if (running.get()) {
            return;
        }
        if (cliCommand == null) {
            appendOutput("[ERROR] El CLI no está disponible.\n");
            statusLabel.setText("CLI no disponible");
            return;
        }

        Path workDir = workingDirectory.get();
        if (workDir == null) {
            appendOutput("[ERROR] No hay proyecto seleccionado.\n");
            statusLabel.setText("Selecciona un proyecto");
            return;
        }

        List<String> command = cliCommand.command(List.of());

        output.clear();
        appendOutput("[Iniciando consola PTY…]\n");
        appendOutput("[Directorio: " + workDir + "]\n");
        appendOutput("[Comando: " + String.join(" ", command) + "]\n\n");

        try {
            PtySession current = new Pty4jSession(command, workDir, 80, 24);
            session = current;
            current.start();
            running.set(true);

            readerThread = Thread.ofVirtual().start(() -> readOutput(current));

            Thread.ofVirtual().start(() -> {
                try {
                    int exitCode = current.waitFor();
                    Platform.runLater(() -> {
                        if (session != current) {
                            return;
                        }
                        running.set(false);
                        appendOutput("\n\n[Proceso terminado con código " + exitCode + "]\n");
                        statusLabel.setText("Código de salida: " + exitCode);
                        updateButtonStates();
                    });
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });

            statusLabel.setText("Consola activa");
            updateButtonStates();
        } catch (Exception exception) {
            appendOutput("[ERROR] No se pudo iniciar: " + exception.getMessage() + "\n");
            statusLabel.setText("Error al iniciar");
            running.set(false);
            updateButtonStates();
        }
    }

    /** Enfoca el campo de entrada de la consola. */
    public void focusInput() {
        input.requestFocus();
    }

    private void readOutput(PtySession current) {
        try {
            byte[] buffer = new byte[4096];
            while (running.get() && session == current && current.isAlive()) {
                int bytesRead = current.getInputStream().read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                if (bytesRead > 0) {
                    String text = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    String clean = AnsiStripper.strip(text);
                    Platform.runLater(() -> appendOutput(clean));
                }
            }
        } catch (IOException exception) {
            if (running.get() && session == current) {
                Platform.runLater(() -> appendOutput("\n[Error de lectura: " + exception.getMessage() + "]\n"));
            }
        }
    }

    private void sendLine() {
        String line = input.getText();
        if (line == null || line.isBlank()) {
            return;
        }
        if (!running.get() || session == null || !session.isAlive()) {
            appendOutput("[ERROR] La consola no está activa.\n");
            return;
        }

        try {
            OutputStream out = session.getOutputStream();
            out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            input.clear();
            appendOutput("> " + line + "\n");
        } catch (IOException exception) {
            appendOutput("[ERROR al enviar: " + exception.getMessage() + "]\n");
        }
    }

    private void sendInterrupt() {
        if (!running.get() || session == null || !session.isAlive()) {
            appendOutput("[ERROR] La consola no está activa.\n");
            return;
        }
        session.interrupt();
        appendOutput("[Ctrl+C enviado]\n");
    }

    private void restart() {
        close();
        startSession();
    }

    private void close() {
        boolean wasActive = running.getAndSet(false);
        if (session != null) {
            session.close();
            session = null;
        }
        if (wasActive) {
            appendOutput("\n[Consola cerrada]\n");
        }
        statusLabel.setText("Consola cerrada");
        updateButtonStates();
    }

    private void clearOutput() {
        output.clear();
    }

    private void appendOutput(String text) {
        output.appendText(text);
        output.setScrollTop(Double.MAX_VALUE);
    }

    private void updateButtonStates() {
        boolean active = running.get();
        startButton.setDisable(active || cliCommand == null);
        startButton.setManaged(!active);
        startButton.setVisible(!active);
        input.setDisable(!active);
        sendButton.setDisable(!active);
        interruptButton.setDisable(!active);
        closeButton.setDisable(!active);
        restartButton.setDisable(cliCommand == null);

        statusDot.getStyleClass().removeAll("on", "off");
        statusDot.getStyleClass().add(active ? "on" : "off");
        statusLabel.getStyleClass().removeAll("muted", "ok");
        statusLabel.getStyleClass().add(active ? "ok" : "muted");

        onStateChanged.accept(active);
    }

    /** Indica si la consola está activa. */
    public boolean isActive() {
        return running.get();
    }

    /** Cierra la sesión si está activa. */
    public void shutdown() {
        close();
    }
}
