package es.vargontoc.agentic.develop.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** init de extremo a extremo. */
class InitCommandTest {

    private static final JsonMapper JSON = JsonMapperFactory.create();

    @TempDir
    Path tmp;

    Path project;
    CommandLine cli;

    @BeforeEach
    void setUp() throws IOException {
        Path home = Files.createDirectories(tmp.resolve("home"));
        project = Files.createDirectories(tmp.resolve("proyecto"));
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
    void createsTheSddStructureAndReportsItAsCreated() {
        Map<?, ?> out = run(0, "init", "--project", project.toString());

        assertEquals("init", out.get("command"));
        assertEquals(true, data(out).get("created"));
        assertEquals(false, data(out).get("alreadyConfigured"));
        assertTrue(Files.isDirectory(project.resolve(".sdd")));
    }

    @Test
    void aSecondRunChangesNothing() throws IOException {
        run(0, "init", "--project", project.toString());
        Path team = project.resolve(".sdd/team.md");
        Files.writeString(team, "editado por el usuario");

        Map<?, ?> out = run(0, "init", "--project", project.toString());

        assertEquals(false, data(out).get("created"));
        assertEquals(true, data(out).get("alreadyConfigured"));
        assertEquals("editado por el usuario", Files.readString(team));
    }

    @Test
    void aMissingPathIsAnErrorWithThePathAsTyped() {
        String missing = tmp.resolve("no-existe").toString();

        Map<?, ?> out = run(1, "init", "--project", missing);

        assertEquals("not_found.project.path", error(out).get("code"));
        assertEquals(missing, error(out).get("path"));
    }

    @Test
    void aFileInsteadOfADirectoryIsAnError() throws IOException {
        Path file = Files.writeString(tmp.resolve("fichero"), "x");

        Map<?, ?> out = run(1, "init", "--project", file.toString());

        assertEquals(file.toString(), error(out).get("path"));
        assertTrue(Files.notExists(tmp.resolve(".sdd")));
    }

    @Test
    void theProjectOptionIsRequired() {
        assertEquals("usage.missing_option", error(run(2, "init")).get("code"));
    }
}
