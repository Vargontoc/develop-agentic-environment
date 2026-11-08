package es.vargontoc.agentic.develop.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

/** create-artifact de extremo a extremo. */
class CreateArtifactCommandTest {

    private static final JsonMapper JSON = JsonMapperFactory.create();

    @TempDir
    Path tmp;

    Path home;
    Path project;
    CommandLine cli;
    byte[] stdin = new byte[0];

    @BeforeEach
    void setUp() throws IOException {
        home = Files.createDirectories(tmp.resolve("home"));
        project = Files.createDirectories(tmp.resolve("proyecto"));
        Services services = Services.of(new PathResolver(Map.of("HOME", home.toString()), PathResolver.Os.LINUX));
        cli = new CommandLine(Commands.standard(() -> services, () -> (InputStream) new ByteArrayInputStream(stdin)));
    }

    private Map<?, ?> run(int expectedExit, String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        assertEquals(expectedExit, cli.run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8)));
        return JSON.readValue(buffer.toString(StandardCharsets.UTF_8), Map.class);
    }

    private Map<?, ?> create(int exit, String provider, String scope, String kind, String name, String... extra) {
        String[] base = {"create-artifact", "--provider", provider, "--scope", scope, "--kind", kind, "--name", name,
            "--project", project.toString()};
        return run(exit, Stream.concat(Stream.of(base), Stream.of(extra)).toArray(String[]::new));
    }

    private static Map<?, ?> data(Map<?, ?> envelope) {
        return (Map<?, ?>) envelope.get("data");
    }

    private static String errorCode(Map<?, ?> envelope) {
        return (String) ((Map<?, ?>) ((List<?>) envelope.get("errors")).get(0)).get("code");
    }

    private Path content(String text) throws IOException {
        return Files.writeString(tmp.resolve("contenido-" + System.nanoTime() + ".md"), text);
    }

    private Path agent() {
        return home.resolve(".claude/agents/sdd-design.md");
    }

    @Test
    void anAgentFromTheUnmodifiedTemplateIsCreatedAndShowsUpInTheTree() throws IOException {
        Map<?, ?> out = create(0, "claude", "global", "agent", "sdd-design");

        assertEquals("create-artifact", out.get("command"));
        assertEquals(false, data(out).get("replaced"));
        assertEquals(false, data(out).get("custom"));
        assertTrue(Files.isRegularFile(agent()));
        assertTrue(Files.readString(agent()).contains("name: sdd-design"));
        assertTrue(run(0, "tree").toString().contains("sdd-design"));
        Map<?, ?> described = run(0, "describe", "--path", agent().toString());
        assertEquals("linked", data(described).get("status"));
    }

    @Test
    void modifiedContentReplacesTheBodyKeepingTheHeader() throws IOException {
        Map<?, ?> out = create(0, "claude", "global", "agent", "sdd-design", "--content-file", content("Mi cuerpo\n").toString());

        assertEquals(true, data(out).get("custom"));
        var parts = ArtifactFormat.MARKDOWN.split(Files.readString(agent()));
        assertEquals("Mi cuerpo\n", parts.body());
        assertTrue(parts.header().contains("name: sdd-design"));
    }

    @Test
    void contentCanComeFromStandardInput() throws IOException {
        stdin = "Desde stdin\n".getBytes(StandardCharsets.UTF_8);

        create(0, "claude", "global", "agent", "sdd-design", "--stdin");

        assertEquals("Desde stdin\n", ArtifactFormat.MARKDOWN.split(Files.readString(agent())).body());
    }

    @Test
    void anExistingNameIsAConflictAndOverwriteKeepsABackup() throws IOException {
        create(0, "claude", "global", "agent", "sdd-design", "--content-file", content("uno\n").toString());
        String before = Files.readString(agent());

        Map<?, ?> conflict = create(1, "claude", "global", "agent", "sdd-design", "--content-file", content("dos\n").toString());
        assertEquals("conflict.artifact.already_exists", errorCode(conflict));
        assertEquals(before, Files.readString(agent()));

        Map<?, ?> forced = create(0, "claude", "global", "agent", "sdd-design", "--content-file", content("dos\n").toString(),
            "--overwrite");
        assertEquals(true, data(forced).get("replaced"));
        assertNotNull(data(forced).get("backup"));
        assertEquals("dos\n", ArtifactFormat.MARKDOWN.split(Files.readString(agent())).body());
    }

    @Test
    void aSkillIsCreatedCompleteWithItsAssetsAndWithModifiedContent() throws IOException {
        Map<?, ?> codex = create(0, "codex", "global", "skill", "sdd-design");
        Path skill = home.resolve(".agents/skills/sdd-design");
        assertTrue(Files.isRegularFile(skill.resolve("SKILL.md")));
        assertTrue(Files.isRegularFile(skill.resolve("agents/openai.yaml")));
        assertEquals("skill", data(codex).get("kind"));

        create(0, "claude", "global", "skill", "sdd-requirements", "--content-file", content("# Propia\n").toString());
        assertEquals("# Propia\n", Files.readString(home.resolve(".claude/skills/sdd-requirements/SKILL.md")).replace("\r\n", "\n"));
        assertEquals("conflict.artifact.already_exists", errorCode(create(1, "claude", "global", "skill", "sdd-requirements")));
    }

    @Test
    void aProjectSpecialistIsCreatedOnlyForARegisteredModule() throws IOException {
        run(0, "init", "--project", project.toString());
        run(0, "create-module", "--project", project.toString(), "--module", "alfa", "--technicians", "analista",
            "--providers", "claude");
        Files.delete(project.resolve(".claude/agents/analista-alfa.md"));

        Map<?, ?> out = create(0, "claude", "project", "agent", "analista-alfa");
        assertTrue(Files.readString(project.resolve(".claude/agents/analista-alfa.md")).contains("alfa"));
        assertEquals("project", data(out).get("scope"));

        assertEquals("validation.artifact.not_catalogued", errorCode(create(1, "claude", "project", "agent", "analista-beta")));
    }

    @Test
    void invalidProviderScopeKindAndNameAreErrorsAndWriteNothing() throws IOException {
        assertEquals("validation.provider.unknown", errorCode(create(1, "otro", "global", "agent", "sdd-design")));
        assertEquals("usage.invalid_value", errorCode(create(2, "claude", "mundial", "agent", "sdd-design")));
        assertEquals("usage.invalid_value", errorCode(create(2, "claude", "global", "perfil", "sdd-design")));
        assertEquals("validation.artifact.not_catalogued", errorCode(create(1, "claude", "global", "agent", "no-existe")));
        assertEquals("usage.missing_option", errorCode(run(2, "create-artifact", "--provider", "claude")));
        assertFalse(Files.exists(home.resolve(".claude/agents")));
    }
}
