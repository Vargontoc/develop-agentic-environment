package es.vargontoc.agentic.viewer.setup;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias para ConfigureProvidersDialog.
 */
class ConfigureProvidersDialogTest {

    @Test
    void providerInfoContainsAllSupportedProviders() {
        // Verificar que se construyen los 4 proveedores soportados
        List<ConfigureProvidersDialog.ProviderInfo> infos = buildProviderInfos(List.of());
        
        assertEquals(4, infos.size(), "Debe haber 4 proveedores soportados");
        
        List<String> ids = infos.stream().map(ConfigureProvidersDialog.ProviderInfo::id).toList();
        assertTrue(ids.contains("claude"), "Debe incluir claude");
        assertTrue(ids.contains("codex"), "Debe incluir codex");
        assertTrue(ids.contains("gemini"), "Debe incluir gemini");
        assertTrue(ids.contains("opencode"), "Debe incluir opencode");
    }

    @Test
    void providerInfoMarksConfiguredProviders() {
        List<ConfigureProvidersDialog.ProviderInfo> infos = buildProviderInfos(List.of("claude", "opencode"));
        
        ConfigureProvidersDialog.ProviderInfo claude = infos.stream()
            .filter(i -> "claude".equals(i.id()))
            .findFirst()
            .orElseThrow();
        assertTrue(claude.configured(), "Claude debe estar marcado como configurado");
        
        ConfigureProvidersDialog.ProviderInfo codex = infos.stream()
            .filter(i -> "codex".equals(i.id()))
            .findFirst()
            .orElseThrow();
        assertFalse(codex.configured(), "Codex no debe estar marcado como configurado");
    }

    @Test
    void providerInfoHasDescriptiveNames() {
        List<ConfigureProvidersDialog.ProviderInfo> infos = buildProviderInfos(List.of());
        
        for (ConfigureProvidersDialog.ProviderInfo info : infos) {
            assertNotNull(info.name(), "El nombre no debe ser null");
            assertFalse(info.name().isEmpty(), "El nombre no debe estar vacío");
            assertNotNull(info.description(), "La descripción no debe ser null");
            assertFalse(info.description().isEmpty(), "La descripción no debe estar vacía");
        }
    }

    @Test
    void resultContainsSelectedProvidersAndResults() {
        List<String> selected = List.of("claude", "gemini");
        List<ConfigureProvidersDialog.ProviderResult> results = List.of(
            new ConfigureProvidersDialog.ProviderResult("claude", "Claude", 10, 5, List.of()),
            new ConfigureProvidersDialog.ProviderResult("gemini", "Gemini", 8, 3, List.of("warning1"))
        );
        
        ConfigureProvidersDialog.Result result = new ConfigureProvidersDialog.Result(selected, results);
        
        assertEquals(2, result.selectedProviders().size());
        assertEquals(2, result.results().size());
        assertEquals("claude", result.results().get(0).providerId());
        assertEquals(10, result.results().get(0).created());
        assertEquals(5, result.results().get(0).skipped());
        assertEquals(1, result.results().get(1).warnings().size());
    }

    @Test
    void providerResultHandlesWarnings() {
        ConfigureProvidersDialog.ProviderResult result = new ConfigureProvidersDialog.ProviderResult(
            "claude", "Claude", 5, 2, List.of("warning1", "warning2")
        );
        
        assertEquals("claude", result.providerId());
        assertEquals("Claude", result.providerName());
        assertEquals(5, result.created());
        assertEquals(2, result.skipped());
        assertEquals(2, result.warnings().size());
        assertTrue(result.warnings().contains("warning1"));
        assertTrue(result.warnings().contains("warning2"));
    }

    /**
     * Helper para construir ProviderInfo (refleja la lógica del diálogo).
     */
    private List<ConfigureProvidersDialog.ProviderInfo> buildProviderInfos(List<String> currentProviders) {
        return List.of(
            new ConfigureProvidersDialog.ProviderInfo("claude", "Claude", 
                "Instala agentes y skills en ~/.claude/ (y el hook sdd-specs-only) para usar con Claude Desktop y Claude Code.",
                currentProviders.contains("claude")),
            new ConfigureProvidersDialog.ProviderInfo("codex", "Codex",
                "Instala agentes y skills en ~/.codex/ y ~/.agents/ para usar con Codex CLI.",
                currentProviders.contains("codex")),
            new ConfigureProvidersDialog.ProviderInfo("gemini", "Gemini",
                "Instala agentes y skills en ~/.gemini/config/ y ~/.agents/ para usar con Gemini CLI.",
                currentProviders.contains("gemini")),
            new ConfigureProvidersDialog.ProviderInfo("opencode", "OpenCode",
                "Instala agentes y skills en ~/.config/opencode/ para usar con OpenCode.",
                currentProviders.contains("opencode"))
        );
    }
}
