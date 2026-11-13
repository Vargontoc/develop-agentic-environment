package es.vargontoc.agentic.develop.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
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

/** `team` y `team-update` de extremo a extremo sobre un proyecto iniciado. */
class TeamCommandsTest {

    private static final JsonMapper JSON = JsonMapperFactory.create();

    @TempDir
    Path tmp;

    Path project;
    Services services;

    @BeforeEach
    void setUp() throws IOException {
        Path home = Files.createDirectories(tmp.resolve("home"));
        project = Files.createDirectories(tmp.resolve("proyecto"));
        services = Services.of(new PathResolver(Map.of("HOME", home.toString()), PathResolver.Os.LINUX));
        run(null, 0, "init", "--project", project.toString());
    }

    @Test
    void teamReturnsModulesAndEffectiveOptions() {
        Map<?, ?> out = run(null, 0, "team", "--project", project.toString());

        Map<?, ?> data = data(out);
        assertEquals(0, ((List<?>) data.get("modules")).size());
        Map<?, ?> options = (Map<?, ?>) data.get("options");
        assertEquals("si", options.get("sprints"));
        assertEquals("balanced", options.get("gitPolicy"));
        assertEquals("implementation-first", options.get("contractStrategy"));
        assertEquals(null, options.get("readmeOwner"));
        assertFalse(((String) data.get("hash")).isBlank());
    }

    @Test
    void dryRunReturnsDiffAndDoesNotWrite() throws IOException {
        Path team = project.resolve(".sdd/team.md");
        String before = Files.readString(team);

        Map<?, ?> out = run(payload(), 0, "team-update", "--project", project.toString(),
            "--stdin", "--dry-run");

        Map<?, ?> data = data(out);
        assertEquals(true, data.get("dryRun"));
        assertEquals(true, data.get("written"));
        assertTrue(((String) data.get("diff")).contains("- alfa | ALFA | src/alfa"));
        assertEquals(null, data.get("backup"));
        assertEquals(before, Files.readString(team));
    }

    @Test
    void updateWritesModulesOptionsAndBackup() throws IOException {
        Map<?, ?> out = run(payload(), 0, "team-update", "--project", project.toString(), "--stdin");

        Map<?, ?> data = data(out);
        assertEquals(true, data.get("written"));
        assertNotNull(data.get("backup"));
        String content = Files.readString(project.resolve(".sdd/team.md"));
        assertTrue(content.contains("- alfa | ALFA | src/alfa"));
        assertTrue(content.contains("- sprints: no"));
        assertTrue(content.contains("- readme-owner: alfa"));

        Map<?, ?> team = data(run(null, 0, "team", "--project", project.toString()));
        List<?> modules = (List<?>) team.get("modules");
        assertEquals("alfa", ((Map<?, ?>) modules.getFirst()).get("name"));
        assertEquals("no", ((Map<?, ?>) team.get("options")).get("sprints"));
        assertEquals("alfa", ((Map<?, ?>) team.get("options")).get("readmeOwner"));
    }

    @Test
    void staleExpectedHashIsAConflict() {
        Map<?, ?> team = data(run(null, 0, "team", "--project", project.toString()));
        String hash = (String) team.get("hash");

        Map<?, ?> out = run(payload(), 1, "team-update", "--project", project.toString(),
            "--stdin", "--expect-hash", "viejo");

        assertEquals("conflict.team.modified_externally", errorCode(out));
        assertFalse(hash.isBlank());
    }

    @Test
    void invalidPayloadIsAContentError() {
        Map<?, ?> out = run("{no es json", 1, "team-update", "--project", project.toString(), "--stdin");

        assertEquals("validation.content.unreadable", errorCode(out));
    }

    private String payload() {
        return """
            {"options":{"sprints":"no","gitPolicy":"strict","contractStrategy":"contract-first","readmeOwner":"alfa"},
             "modules":[{"name":"alfa","mapId":"ALFA","paths":["src/alfa"]}]}
            """;
    }

    private Map<?, ?> run(String stdin, int expectedExit, String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        CommandLine cli = new CommandLine(Commands.standard(() -> services,
            () -> new ByteArrayInputStream((stdin == null ? "" : stdin).getBytes(StandardCharsets.UTF_8))));
        assertEquals(expectedExit, cli.run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8)));
        return JSON.readValue(buffer.toString(StandardCharsets.UTF_8), Map.class);
    }

    private static Map<?, ?> data(Map<?, ?> envelope) {
        return (Map<?, ?>) envelope.get("data");
    }

    private static String errorCode(Map<?, ?> envelope) {
        return (String) ((Map<?, ?>) ((List<?>) envelope.get("errors")).getFirst()).get("code");
    }
}
