package es.vargontoc.agentic.develop.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

/** configure-provider de extremo a extremo con un home temporal. */
class ConfigureProviderCommandTest {

    private static final JsonMapper JSON = JsonMapperFactory.create();

    @TempDir
    Path tmp;

    Path home;
    CommandLine cli;

    @BeforeEach
    void setUp() throws IOException {
        home = Files.createDirectories(tmp.resolve("home"));
        restart();
    }

    // Cada invocación real de la CLI es un proceso nuevo: el almacén de metadatos no conserva nada en memoria entre ellas
    private void restart() {
        Services services = Services.of(new PathResolver(Map.of("HOME", home.toString()), PathResolver.Os.LINUX));
        cli = new CommandLine(Commands.standard(() -> services));
    }

    private Map<?, ?> run(int expectedExit, String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        assertEquals(expectedExit, cli.run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8)));
        return JSON.readValue(buffer.toString(StandardCharsets.UTF_8), Map.class);
    }

    private static Map<?, ?> data(Map<?, ?> envelope) {
        return (Map<?, ?>) envelope.get("data");
    }

    private static Map<?, ?> error(Map<?, ?> envelope) {
        return (Map<?, ?>) ((List<?>) envelope.get("errors")).get(0);
    }

    @Test
    void anEmptyProviderReceivesAgentsAndSkillsAndTheResultListsThem() {
        Map<?, ?> out = run(0, "configure-provider", "--provider", "claude");

        assertEquals("configure-provider", out.get("command"));
        assertEquals(true, out.get("ok"));
        assertEquals("claude", data(out).get("provider"));
        List<?> created = (List<?>) data(out).get("created");
        assertTrue(created.contains("agents/sdd-agent.md"));
        assertTrue(created.contains("skills/sdd-design"));
        assertTrue(((List<?>) data(out).get("skipped")).isEmpty());
        assertTrue(Files.isRegularFile(home.resolve(".claude/agents/sdd-agent.md")));
        assertTrue(Files.isRegularFile(home.resolve(".claude/skills/sdd-design/SKILL.md")));
    }

    @Test
    void aSecondRunOverwritesNothingAndIsIdempotent() throws IOException {
        run(0, "configure-provider", "--provider", "claude");
        Path agent = home.resolve(".claude/agents/sdd-agent.md");
        Files.writeString(agent, "personalizado");

        Map<?, ?> again = run(0, "configure-provider", "--provider", "claude");

        assertTrue(((List<?>) data(again).get("created")).isEmpty());
        assertTrue(((List<?>) data(again).get("skipped")).contains("agents/sdd-agent.md"));
        assertEquals("personalizado", Files.readString(agent));
    }

    @Test
    void codexGetsItsProfileToo() {
        Map<?, ?> out = run(0, "configure-provider", "--provider", "codex");

        assertTrue(((List<?>) data(out).get("created")).contains("profiles/sdd.config.toml"));
        assertTrue(Files.isRegularFile(home.resolve(".codex/profiles/sdd.config.toml")));
        assertTrue(Files.isRegularFile(home.resolve(".agents/skills/sdd-design/agents/openai.yaml")));
    }

    // --- avisos del hook en la salida cli-json (REQ-006, REQ-007, REQ-008) ---

    private static List<String> hookWarnings(Map<?, ?> out) {
        return ((List<?>) data(out).get("warnings")).stream().map(String::valueOf)
            .filter(w -> w.startsWith("hooks/")).toList();
    }

    private static List<String> hookWarningsAbout(Map<?, ?> out, String text) {
        return hookWarnings(out).stream().filter(w -> w.contains(text)).toList();
    }

    @Test
    void aModifiedHookAppearsInTheWarningsWithFileAndReasonAndDoesNotChangeTheExitCode() throws IOException {
        Map<?, ?> clean = run(0, "configure-provider", "--provider", "claude");
        assertTrue(hookWarningsAbout(clean, "modificado").isEmpty());
        Path hook = home.resolve(".claude/hooks/sdd-specs-only.sh");
        Files.writeString(hook, Files.readString(hook) + "# cambio del usuario\n");

        Map<?, ?> out = run(0, "configure-provider", "--provider", "claude");

        assertEquals(true, out.get("ok"));
        List<String> warnings = hookWarningsAbout(out, "modificado por el usuario");
        assertEquals(1, warnings.size(), String.valueOf(data(out).get("warnings")));
        assertTrue(warnings.get(0).startsWith("hooks/sdd-specs-only.sh"));
        assertTrue(Files.readString(hook).endsWith("# cambio del usuario\n"));
    }

    @Test
    void theRestOfTheProviderIsInstalledNormallyWhenTheHookWarns() throws IOException {
        run(0, "configure-provider", "--provider", "claude");
        Path hook = home.resolve(".claude/hooks/sdd-specs-only.sh");
        Files.writeString(hook, "cambiado\n");
        Files.delete(home.resolve(".claude/agents/sdd-agent.md"));

        Map<?, ?> out = run(0, "configure-provider", "--provider", "claude");

        assertFalse(hookWarningsAbout(out, "modificado").isEmpty());
        assertTrue(((List<?>) data(out).get("created")).contains("agents/sdd-agent.md"));
        assertTrue(Files.isRegularFile(home.resolve(".claude/agents/sdd-agent.md")));
        assertEquals("cambiado\n", Files.readString(hook));
    }

    @Test
    void aHookWithoutARecordAppearsInTheWarningsOnlyForClaude() throws IOException {
        run(0, "configure-provider", "--provider", "claude");
        // Sin almacén de metadatos el hook de Claude queda sin registro; Codex no tiene hook
        Files.delete(home.resolve(".local/state/agentic-develop/metadata.json"));
        restart();

        Map<?, ?> claude = run(0, "configure-provider", "--provider", "claude");
        Map<?, ?> codex = run(0, "configure-provider", "--provider", "codex");

        assertEquals(1, hookWarningsAbout(claude, "no se puede comprobar si fue modificado").size());
        assertTrue(hookWarningsAbout(codex, "hooks/").isEmpty());
        assertFalse(((List<?>) data(codex).get("created")).stream()
            .anyMatch(v -> String.valueOf(v).startsWith("hooks/")));
        assertFalse(Files.exists(home.resolve(".codex/hooks/sdd-specs-only.sh")));
    }

    @Test
    void theWarningsOfAHookDoNotMakeTheCommandFail() throws IOException {
        run(0, "configure-provider", "--provider", "claude");
        Files.writeString(home.resolve(".claude/hooks/sdd-specs-only.sh"), "cambiado\n");
        Files.delete(home.resolve(".local/state/agentic-develop/metadata.json"));
        restart();

        Map<?, ?> out = run(0, "configure-provider", "--provider", "claude");

        assertEquals(true, out.get("ok"));
        assertTrue(((List<?>) out.get("errors")).isEmpty());
        assertEquals(1, hookWarningsAbout(out, "no se puede comprobar si fue modificado").size());
    }

    @Test
    void anUnknownProviderIsAnErrorAndWritesNothing() {
        Map<?, ?> out = run(1, "configure-provider", "--provider", "otro");

        assertEquals("validation.provider.unknown", error(out).get("code"));
        assertEquals("otro", error(out).get("path"));
        assertFalse(Files.exists(home.resolve(".claude")));
    }

    @Test
    void theProviderOptionIsRequired() {
        assertEquals("usage.missing_option", error(run(2, "configure-provider")).get("code"));
    }
}
