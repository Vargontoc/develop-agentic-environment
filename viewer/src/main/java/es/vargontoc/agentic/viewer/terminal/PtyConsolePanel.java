package es.vargontoc.agentic.viewer.terminal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import es.vargontoc.agentic.viewer.cli.CliCommand;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Panel de consola PTY que ejecuta el CLI interactivo. Muestra la salida en
 * un área de texto, permite enviar líneas, redimensionar, interrumpir, cerrar
 * y reiniciar la sesión.
 */
public final class PtyConsolePanel extends VBox {

    private final CliCommand cliCommand;
    private final Supplier<Path> workingDirectory;

    private final TextArea output = new TextArea();
    private final TextField input = new TextField();
    private final Button sendButton = new Button("Enviar");
    private final Button interruptButton = new Button("Interrupt");
    private final Button restartButton = new Button("Reiniciar");
    private final Button closeButton = new Button("Cerrar");
    private final Button clearButton = new Button("Limpiar");
    private final Label statusLabel = new Label("Consola cerrada");

    private PtySession session;
    private Thread readerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

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
        setPadding(new Insets(10));
        setSpacing(8);

        Label title = new Label("Consola PTY");
        title.getStyleClass().add("heading");

        // Área de salida
        output.setEditable(false);
        output.setWrapText(false);
        output.getStyleClass().add("console");
        output.setPrefRowCount(12);
        VBox.setVgrow(output, Priority.ALWAYS);

        // Barra de entrada
        input.setPromptText("Escribe una opción del menú o comando...");
        input.setOnAction(event -> sendLine());
        input.setDisable(true);

        sendButton.setOnAction(event -> sendLine());
        interruptButton.setOnAction(event -> sendInterrupt());
        restartButton.setOnAction(event -> restart());
        closeButton.setOnAction(event -> close());
        clearButton.setOnAction(event -> clearOutput());

        HBox inputBar = new HBox(6, input, sendButton);
        HBox.setHgrow(input, Priority.ALWAYS);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        HBox controlBar = new HBox(6, interruptButton, restartButton, closeButton, clearButton);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel.getStyleClass().add("muted");
        statusLabel.setWrapText(true);

        getChildren().addAll(title, output, inputBar, controlBar, statusLabel);
    }

    /** Inicia la sesión PTY con el CLI interactivo. */
    public void startSession() {
        if (running.get()) {
            return;
        }

        Path workDir = workingDirectory.get();
        if (workDir == null) {
            appendOutput("[ERROR] No hay proyecto seleccionado.\n");
            statusLabel.setText("Error: sin proyecto");
            return;
        }

        // Construir el comando: java -jar agentic-develop.jar (sin argumentos = interactivo)
        List<String> command = cliCommand.command(List.of());

        output.clear();
        appendOutput("[Iniciando consola PTY...]\n");
        appendOutput("[Directorio: " + workDir + "]\n");
        appendOutput("[Comando: " + String.join(" ", command) + "]\n\n");

        try {
            session = new Pty4jSession(command, workDir, 80, 24);
            session.start();
            running.set(true);

            // Hilo lector de salida
            readerThread = Thread.ofVirtual().start(this::readOutput);

            // Hilo que espera el final del proceso
            Thread.ofVirtual().start(() -> {
                try {
                    int exitCode = session.waitFor();
                    Platform.runLater(() -> {
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

    private void readOutput() {
        try {
            byte[] buffer = new byte[4096];
            while (running.get() && session != null && session.isAlive()) {
                int bytesRead = session.getInputStream().read(buffer);
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
            if (running.get()) {
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
            appendOutput("[ERROR] Consola no activa.\n");
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
            appendOutput("[ERROR] Consola no activa.\n");
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
        if (session != null) {
            running.set(false);
            session.close();
            session = null;
            appendOutput("\n[Consola cerrada]\n");
            statusLabel.setText("Consola cerrada");
            updateButtonStates();
        }
    }

    private void clearOutput() {
        output.clear();
    }

    private void appendOutput(String text) {
        output.appendText(text);
        // Auto-scroll al final
        output.setScrollTop(Double.MAX_VALUE);
    }

    private void updateButtonStates() {
        boolean active = running.get();
        input.setDisable(!active);
        sendButton.setDisable(!active);
        interruptButton.setDisable(!active);
        closeButton.setDisable(!active);
        restartButton.setDisable(false);
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
