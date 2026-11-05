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

        assertTrue(((List<?>) data(out).get("created")).contains("sdd.config.toml"));
        assertTrue(Files.isRegularFile(home.resolve(".codex/sdd.config.toml")));
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
