package es.vargontoc.agentic.viewer.setup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import es.vargontoc.agentic.viewer.cli.CliClient;
import es.vargontoc.agentic.viewer.cli.model.ConfigureProviderData;
import es.vargontoc.agentic.viewer.ui.Theme;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Diálogo para configurar proveedores. Muestra los 4 proveedores soportados
 * (Claude, Codex, Gemini, OpenCode) con checkboxes y su estado actual.
 * Permite seleccionar cuáles configurar y ejecuta configure-provider para cada uno.
 * 
 * Se usa tanto para el asistente de primer arranque como para reconfigurar
 * desde el menú Archivo → Configurar proveedores.
 */
public final class ConfigureProvidersDialog {

    private ConfigureProvidersDialog() {}

    /**
     * Información de un proveedor.
     */
    public record ProviderInfo(String id, String name, String description, boolean configured) {}

    /**
     * Resultado de configurar un proveedor.
     */
    public record ProviderResult(String providerId, String providerName, int created, int skipped, List<String> warnings) {}

    /**
     * Resultado completo del diálogo.
     */
    public record Result(List<String> selectedProviders, List<ProviderResult> results) {}

    /**
     * Muestra el diálogo de configuración de proveedores.
     *
     * @param owner        ventana propietaria
     * @param cliClient    cliente CLI para ejecutar configure-provider
     * @param currentProviders lista de proveedores actualmente configurados (del status)
     * @return resultado con la configuración de cada proveedor; vacío si se cancela
     */
    public static java.util.Optional<Result> show(Window owner, CliClient cliClient, List<String> currentProviders) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Configurar proveedores");
        dialog.setHeaderText("Instala los agentes y skills globales en cada proveedor.\n" +
            "Los proveedores no configurados aparecerán marcados en rojo.");
        Theme.apply(dialog);

        ButtonType configure = new ButtonType("Configurar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(configure, cancel);

        VBox content = new VBox(16);
        content.setPadding(new Insets(12));
        content.setPrefWidth(500);

        // Información de los 4 proveedores soportados
        List<ProviderInfo> providers = buildProviderInfos(currentProviders);

        Map<String, CheckBox> checks = new LinkedHashMap<>();
        Map<String, ProviderInfo> infoMap = new LinkedHashMap<>();
        
        for (ProviderInfo info : providers) {
            infoMap.put(info.id(), info);
            
            VBox providerBox = new VBox(4);
            providerBox.getStyleClass().add("provider-row");
            
            HBox header = new HBox(8);
            CheckBox cb = new CheckBox();
            cb.setSelected(!info.configured()); // Seleccionar por defecto los no configurados
            cb.setText(info.name());
            cb.setStyle("-fx-font-weight: bold;");
            checks.put(info.id(), cb);
            
            Label statusLabel = new Label();
            if (info.configured()) {
                statusLabel.setText("✓ Configurado");
                statusLabel.getStyleClass().add("status-configured");
                statusLabel.setStyle("-fx-text-fill: #2e7d32;");
            } else {
                statusLabel.setText("⚠ No configurado");
                statusLabel.getStyleClass().add("status-not-configured");
                statusLabel.setStyle("-fx-text-fill: #d32f2f;");
            }
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            header.getChildren().addAll(cb, spacer, statusLabel);
            
            Label descLabel = new Label(info.description());
            descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            descLabel.setWrapText(true);
            
            providerBox.getChildren().addAll(header, descLabel);
            content.getChildren().add(providerBox);
        }

        Label infoLabel = new Label("Se instalarán los agentes y skills globales en cada proveedor seleccionado.\n" +
            "Los archivos existentes no se sobrescribirán.");
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        infoLabel.setWrapText(true);
        content.getChildren().add(infoLabel);

        dialog.getDialogPane().setContent(content);

        // Deshabilitar botón de configurar si no hay nada seleccionado
        final javafx.scene.Node configureButton = dialog.getDialogPane().lookupButton(configure);
        configureButton.setDisable(true);

        // Habilitar/deshabilitar según selección
        for (CheckBox cb : checks.values()) {
            cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                boolean anySelected = checks.values().stream().anyMatch(CheckBox::isSelected);
                configureButton.setDisable(!anySelected);
            });
        }

        // Resultado del diálogo: capturar selección
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == configure) {
                List<String> selectedProviders = new ArrayList<>();
                for (Map.Entry<String, CheckBox> entry : checks.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        selectedProviders.add(entry.getKey());
                    }
                }
                return new Result(selectedProviders, new ArrayList<>());
            }
            return null;
        });

        dialog.showAndWait();
        
        Result selection = dialog.getResult();
        if (selection == null || selection.selectedProviders().isEmpty()) {
            return java.util.Optional.empty();
        }
        
        // Ejecutar configuración después de cerrar el diálogo
        List<ProviderResult> results = new ArrayList<>();
        for (String providerId : selection.selectedProviders()) {
            try {
                ConfigureProviderData data = cliClient.configureProvider(providerId);
                ProviderInfo info = infoMap.get(providerId);
                int created = data.created() != null ? data.created().size() : 0;
                int skipped = data.skipped() != null ? data.skipped().size() : 0;
                List<String> warnings = data.warnings() != null ? data.warnings() : List.of();
                results.add(new ProviderResult(providerId, info.name(), created, skipped, warnings));
            } catch (Exception e) {
                results.add(new ProviderResult(providerId, infoMap.get(providerId).name(), 0, 0, 
                    List.of("Error: " + e.getMessage())));
            }
        }
        
        return java.util.Optional.of(new Result(selection.selectedProviders(), results));
    }

    private static List<ProviderInfo> buildProviderInfos(List<String> currentProviders) {
        return List.of(
            new ProviderInfo("claude", "Claude", 
                "Instala agentes y skills en ~/.claude/ (y el hook sdd-specs-only) para usar con Claude Desktop y Claude Code.",
                currentProviders.contains("claude")),
            new ProviderInfo("codex", "Codex",
                "Instala agentes y skills en ~/.codex/ y ~/.agents/ para usar con Codex CLI.",
                currentProviders.contains("codex")),
            new ProviderInfo("gemini", "Gemini",
                "Instala agentes y skills en ~/.gemini/config/ y ~/.agents/ para usar con Gemini CLI.",
                currentProviders.contains("gemini")),
            new ProviderInfo("opencode", "OpenCode",
                "Instala agentes y skills en ~/.config/opencode/ para usar con OpenCode.",
                currentProviders.contains("opencode"))
        );
    }

    /**
     * Muestra un resumen de los resultados de la configuración.
     */
    public static void showResults(Window owner, Result result) {
        if (result == null || result.results().isEmpty()) {
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Resultado de configuración");
        dialog.setHeaderText("Configuración de proveedores completada");
        Theme.apply(dialog);

        ButtonType ok = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok);

        VBox content = new VBox(12);
        content.setPadding(new Insets(12));
        content.setPrefWidth(450);

        int totalCreated = 0;
        int totalSkipped = 0;
        List<String> allWarnings = new ArrayList<>();

        for (ProviderResult res : result.results()) {
            VBox row = new VBox(4);
            row.getStyleClass().add("result-row");
            
            Label nameLabel = new Label(res.providerName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            
            String summary = String.format("Creados: %d, Omitidos: %d", res.created(), res.skipped());
            Label summaryLabel = new Label(summary);
            summaryLabel.setStyle("-fx-text-fill: #666;");
            
            row.getChildren().addAll(nameLabel, summaryLabel);
            
            if (res.warnings() != null && !res.warnings().isEmpty()) {
                for (String warning : res.warnings()) {
                    Label warningLabel = new Label("⚠ " + warning);
                    warningLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11px;");
                    warningLabel.setWrapText(true);
                    row.getChildren().add(warningLabel);
                    allWarnings.add(res.providerName() + ": " + warning);
                }
            }
            
            totalCreated += res.created();
            totalSkipped += res.skipped();
            
            content.getChildren().add(row);
        }

        Label totalLabel = new Label(String.format("Total: %d archivos creados, %d omitidos", totalCreated, totalSkipped));
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        content.getChildren().add(totalLabel);

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }
}
