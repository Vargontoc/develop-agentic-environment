package es.vargontoc.agentic.viewer;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.agentic.viewer.cli.CliClient;
import es.vargontoc.agentic.viewer.cli.CliClientException;
import es.vargontoc.agentic.viewer.cli.CliCommand;
import es.vargontoc.agentic.viewer.cli.CliLocator;
import es.vargontoc.agentic.viewer.cli.CliProcess;
import es.vargontoc.agentic.viewer.cli.CliProcess.CliResult;
import es.vargontoc.agentic.viewer.cli.ErrorPresenter;
import es.vargontoc.agentic.viewer.cli.ErrorPresenter.ErrorNotice;
import es.vargontoc.agentic.viewer.cli.model.DescribeData;
import es.vargontoc.agentic.viewer.cli.model.StatusData;
import es.vargontoc.agentic.viewer.cli.model.TreeData;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Pantalla mínima del viewer. TASK-001 comprueba la ventana, TASK-002 ejecuta
 * el CLI y TASK-003 muestra status/tree/describe tipados y los errores.
 */
public final class App extends Application {

    private CliCommand cliCommand;
    private CliClient cliClient;
    private String cliError;

    @Override
    public void start(Stage stage) {
        try {
            cliCommand = CliLocator.resolve(Path.of("").toAbsolutePath());
            cliClient = new CliClient(new CliProcess(cliCommand), new ObjectMapper());
        } catch (RuntimeException exception) {
            cliError = exception.getMessage();
        }

        Label title = new Label("Agentic Viewer");
        title.getStyleClass().add("title");

        Label subtitle = new Label("JavaFX · TASK-003: cliente del contrato");
        subtitle.getStyleClass().add("subtitle");

        VBox header = new VBox(4, title, subtitle);
        VBox content = new VBox(16, bridgeCard(), cliCard());
        VBox root = new VBox(24, header, content);
        root.getStyleClass().add("app");
        root.setPadding(new Insets(36));

        Scene scene = new Scene(root, 980, 820);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("Agentic Viewer");
        stage.setMinWidth(800);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();

        if (cliError != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(stage);
            alert.setTitle("Error de aplicación");
            alert.setHeaderText("No se encontró el CLI");
            alert.setContentText(cliError);
            alert.showAndWait();
        }
    }

    private VBox bridgeCard() {
        Label heading = new Label("Prueba de la ventana");
        heading.getStyleClass().add("heading");

        Label description = new Label("Comprueba que la interfaz responde y que la lógica se puede probar.");
        description.getStyleClass().add("muted");

        TextField name = new TextField("mundo");
        name.setPromptText("Nombre");

        Button test = new Button("Probar");
        test.setDefaultButton(true);

        Label result = new Label();
        result.getStyleClass().add("result");
        result.setWrapText(true);

        test.setOnAction(event -> {
            result.getStyleClass().removeAll("ok", "error");
            try {
                result.setText(Greeting.greet(name.getText()));
                result.getStyleClass().add("ok");
            } catch (IllegalArgumentException exception) {
                result.setText(exception.getMessage());
                result.getStyleClass().add("error");
            }
        });

        HBox row = new HBox(8, name, test);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(name, Priority.ALWAYS);

        VBox card = new VBox(10, heading, description, row, result);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        return card;
    }

    private VBox cliCard() {
        Label heading = new Label("CLI");
        heading.getStyleClass().add("heading");

        Label path = new Label(cliCommand == null
            ? "No se encontró el jar del CLI"
            : "Jar: " + cliCommand.cliJar());
        path.getStyleClass().add("muted");
        path.setWrapText(true);

        TextArea output = new TextArea();
        output.setEditable(false);
        output.setWrapText(false);
        output.setPrefRowCount(14);
        output.getStyleClass().add("console");

        Label state = new Label(cliError == null ? "Listo" : cliError);
        state.getStyleClass().add(cliError == null ? "muted" : "error");
        state.setWrapText(true);

        Button rawVersion = new Button("--version");
        Button status = new Button("status");
        Button tree = new Button("tree");
        Button clear = new Button("Limpiar");

        rawVersion.setOnAction(event -> runRaw(List.of("--version"), output, state));
        status.setOnAction(event -> runTyped("status",
            () -> formatStatus(cliClient.status(null)), output, state));
        tree.setOnAction(event -> runTyped("tree",
            () -> formatTree(cliClient.tree(null)), output, state));
        clear.setOnAction(event -> {
            output.clear();
            state.setText("Listo");
        });

        HBox primary = new HBox(8, rawVersion, status, tree, clear);
        primary.setAlignment(Pos.CENTER_LEFT);

        TextField artifactPath = new TextField();
        artifactPath.setPromptText("Ruta de un agente o skill para describe");
        Button describe = new Button("describe");
        describe.setOnAction(event -> runTyped("describe",
            () -> formatDescribe(cliClient.describe(pathFrom(artifactPath), null)), output, state));
        HBox describeRow = new HBox(8, artifactPath, describe);
        describeRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(artifactPath, Priority.ALWAYS);

        boolean missing = cliClient == null;
        rawVersion.setDisable(missing);
        status.setDisable(missing);
        tree.setDisable(missing);
        describe.setDisable(missing);
        artifactPath.setDisable(missing);

        VBox card = new VBox(10, heading, path, primary, describeRow, state, output);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        return card;
    }

    private void runRaw(List<String> args, TextArea output, Label state) {
        state.setText("Ejecutando…");
        output.clear();

        Thread.ofVirtual().start(() -> {
            try {
                CliResult result = new CliProcess(cliCommand).run(args);
                String text = formatResult(result);
                Platform.runLater(() -> {
                    output.setText(text);
                    state.setText(result.timedOut() ? "Timeout" : "Código " + result.exitCode());
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    output.setText(exception.getMessage());
                    state.setText("Error");
                });
            }
        });
    }

    private void runTyped(String label, Callable<String> action, TextArea output, Label state) {
        state.setText("Ejecutando…");
        output.clear();

        Thread.ofVirtual().start(() -> {
            try {
                String text = action.call();
                Platform.runLater(() -> {
                    output.setText(text);
                    state.setText(label + " OK");
                });
            } catch (CliClientException exception) {
                ErrorNotice notice = ErrorPresenter.present(exception);
                Platform.runLater(() -> {
                    output.setText(formatNotice(notice));
                    state.setText("Error " + notice.code());
                });
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    output.setText(exception.getMessage());
                    state.setText("Error");
                });
            }
        });
    }

    private static Path pathFrom(TextField field) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Escribe la ruta de un agente o skill.");
        }
        return Path.of(value);
    }

    private static String formatResult(CliResult result) {
        StringBuilder text = new StringBuilder();
        text.append("exit: ").append(result.exitCode())
            .append(" · ").append(result.durationMillis()).append(" ms");
        if (result.timedOut()) {
            text.append(" · TIMEOUT");
        }
        text.append("\n\n=== stdout ===\n").append(result.stdout());
        if (!result.stderr().isBlank()) {
            text.append("\n=== stderr ===\n").append(result.stderr());
        }
        return text.toString();
    }

    private static String formatStatus(StatusData data) {
        StringBuilder text = new StringBuilder("Proveedores:\n");
        if (data.providers() != null) {
            for (StatusData.Provider provider : data.providers()) {
                text.append("  ").append(provider.name())
                    .append(" (").append(provider.id()).append("): ")
                    .append(provider.state());
                if (provider.missing() != null && !provider.missing().isEmpty()) {
                    text.append(" · faltan ").append(provider.missing().size());
                }
                text.append('\n');
            }
        }

        if (data.project() == null) {
            text.append("Proyecto: (sin proyecto)");
        } else {
            text.append("Proyecto: ").append(data.project().path()).append('\n');
            text.append("  inicializado: ").append(data.project().initialized()).append('\n');
            int modules = data.project().modules() == null ? 0 : data.project().modules().size();
            text.append("  módulos: ").append(modules);
            if (data.project().warnings() != null && !data.project().warnings().isEmpty()) {
                text.append(" · avisos: ").append(data.project().warnings().size());
            }
        }
        return text.toString();
    }

    private static String formatTree(TreeData data) {
        StringBuilder text = new StringBuilder();
        appendTree(text, "Global", data.global());
        appendTree(text, "Project", data.project());
        if (data.warnings() != null && !data.warnings().isEmpty()) {
            text.append("Avisos: ").append(data.warnings().size()).append('\n');
        }
        if (data.problems() != null && !data.problems().isEmpty()) {
            text.append("Problemas: ").append(data.problems().size()).append('\n');
        }
        return text.toString();
    }

    private static void appendTree(StringBuilder text, String title, List<TreeData.Node> nodes) {
        text.append(title).append(":\n");
        if (nodes == null || nodes.isEmpty()) {
            text.append("  (vacío)\n");
            return;
        }
        appendNodes(text, nodes, 1);
    }

    private static void appendNodes(StringBuilder text, List<TreeData.Node> nodes, int depth) {
        if (nodes == null) {
            return;
        }
        for (TreeData.Node node : nodes) {
            text.append("  ".repeat(depth))
                .append('[').append(node.type()).append("] ").append(node.name());
            if (node.status() != null) {
                text.append(" (").append(node.status()).append(')');
            }
            if (node.pending()) {
                text.append(" · pendiente");
            }
            text.append('\n');
            appendNodes(text, node.children(), depth + 1);
        }
    }

    private static String formatDescribe(DescribeData data) {
        StringBuilder text = new StringBuilder();
        text.append("provider: ").append(data.provider()).append('\n');
        text.append("scope: ").append(data.scope()).append('\n');
        text.append("kind: ").append(data.kind()).append('\n');
        text.append("name: ").append(data.name()).append('\n');
        text.append("format: ").append(data.format() == null ? "-" : data.format()).append('\n');
        text.append("module: ").append(data.module() == null ? "-" : data.module()).append('\n');
        text.append("status: ").append(data.status()).append('\n');
        text.append("modifiedExternally: ").append(data.modifiedExternally()).append('\n');
        text.append("hash: ").append(data.hash()).append('\n');
        text.append("header: ").append(lineCount(data.header())).append(" líneas\n");
        text.append("body: ").append(lineCount(data.body())).append(" líneas\n");
        text.append("sharedBody: ")
            .append(data.sharedBody() == null ? "-" : lineCount(data.sharedBody()) + " líneas")
            .append('\n');
        text.append("files: ").append(data.files() == null ? 0 : data.files().size());
        return text.toString();
    }

    private static int lineCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) text.lines().count();
    }

    private static String formatNotice(ErrorNotice notice) {
        return "ERROR " + notice.code()
            + "\nRuta: " + (notice.path() == null ? "-" : notice.path())
            + "\nMensaje: " + notice.message()
            + "\nAcción: " + notice.action();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
