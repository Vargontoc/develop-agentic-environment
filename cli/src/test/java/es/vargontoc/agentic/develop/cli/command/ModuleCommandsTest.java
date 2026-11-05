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
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

/** create-module y add-technician de extremo a extremo. */
class ModuleCommandsTest {

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
        run(0, "init", "--project", project.toString());
    }

    private Map<?, ?> run(int expectedExit, String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        assertEquals(expectedExit, cli.run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8)));
        return JSON.readValue(buffer.toString(StandardCharsets.UTF_8), Map.class);
    }

    private Map<?, ?> createModule(int exit, String module, String technicians, String... extra) {
        String[] base = {"create-module", "--project", project.toString(), "--module", module, "--technicians", technicians,
            "--providers", "claude"};
        return run(exit, Stream.concat(Stream.of(base), Stream.of(extra)).toArray(String[]::new));
    }

    private Map<?, ?> addTechnician(int exit, String module, String technicians, String... extra) {
        String[] base = {"add-technician", "--project", project.toString(), "--module", module, "--technicians", technicians,
            "--providers", "claude"};
        return run(exit, Stream.concat(Stream.of(base), Stream.of(extra)).toArray(String[]::new));
    }

    private static Map<?, ?> data(Map<?, ?> envelope) {
        return (Map<?, ?>) envelope.get("data");
    }

    private static String errorCode(Map<?, ?> envelope) {
        return (String) ((Map<?, ?>) ((List<?>) envelope.get("errors")).get(0)).get("code");
    }

    private static boolean anyContains(Object list, String text) {
        return ((List<?>) list).stream().anyMatch(s -> s.toString().contains(text));
    }

    private Path agent(String name) {
        return project.resolve(".claude/agents/" + name + ".md");
    }

    private Path team() {
        return project.resolve(".sdd/team.md");
    }

    private String snapshot() throws IOException {
        try(Stream<Path> files = Files.walk(project)) {
            return files.filter(Files::isRegularFile).map(f -> project.relativize(f) + ":" + f.toFile().length())
                .sorted().toList().toString();
        }
    }

    @Test
    void createModuleRegistersItAndReturnsTheSummary() throws IOException {
        Map<?, ?> out = createModule(0, "Alfa", "analista,desarrollador");

        assertEquals("create-module", out.get("command"));
        assertEquals("alfa", data(out).get("module"));
        assertEquals(false, data(out).get("dryRun"));
        assertTrue(anyContains(data(out).get("created"), "analista-alfa.md"));
        assertTrue(anyContains(data(out).get("created"), "desarrollador-router.md"));
        assertTrue(Files.isRegularFile(agent("analista-alfa")));
        assertTrue(Files.readString(team()).contains("- alfa | - | -"));
    }

    @Test
    void dryRunListsWhatWouldBeCreatedWithoutTouchingTheDisk() throws IOException {
        String before = snapshot();

        Map<?, ?> out = createModule(0, "alfa", "analista", "--dry-run");

        assertEquals(true, data(out).get("dryRun"));
        assertTrue(anyContains(data(out).get("created"), "team.md"));
        assertTrue(anyContains(data(out).get("created"), "analista-alfa.md"));
        assertTrue(anyContains(data(out).get("created"), "skills/"));
        assertEquals(before, snapshot());
        assertFalse(Files.readString(team()).contains("alfa"));

        // La simulación no consumió nada: ejecutar de verdad crea lo mismo que anunció
        Map<?, ?> real = createModule(0, "alfa", "analista");
        assertEquals(((List<?>) data(out).get("created")).stream().filter(s -> s.toString().contains("agents/")).toList(),
            ((List<?>) data(real).get("created")).stream().filter(s -> s.toString().contains("agents/")).toList());
    }

    @Test
    void repeatingCreateModuleIsIdempotent() throws IOException {
        createModule(0, "alfa", "analista");
        Files.writeString(agent("analista-alfa"), "personalizado");

        Map<?, ?> again = createModule(0, "alfa", "analista");

        assertTrue(((List<?>) data(again).get("created")).isEmpty());
        assertTrue(anyContains(data(again).get("skipped"), "analista-alfa.md"));
        assertEquals("personalizado", Files.readString(agent("analista-alfa")));
        assertEquals(1, Files.readAllLines(team()).stream().filter(l -> l.startsWith("- alfa")).count());
    }

    @Test
    void addTechnicianCreatesOnlyTheNewOnesAndLeavesTheRestUntouched() throws IOException {
        createModule(0, "alfa", "analista");
        Files.writeString(agent("analista-alfa"), "personalizado");
        String teamBefore = Files.readString(team());

        Map<?, ?> out = addTechnician(0, "alfa", "desarrollador");

        assertEquals("add-technician", out.get("command"));
        assertTrue(anyContains(data(out).get("created"), "desarrollador-alfa.md"));
        assertTrue(Files.isRegularFile(agent("desarrollador-alfa")));
        assertEquals("personalizado", Files.readString(agent("analista-alfa")));
        assertEquals(teamBefore, Files.readString(team()));

        Map<?, ?> again = addTechnician(0, "alfa", "desarrollador");
        assertTrue(((List<?>) data(again).get("created")).isEmpty());
        assertTrue(anyContains(data(again).get("skipped"), "desarrollador-alfa.md"));
    }

    @Test
    void addTechnicianDryRunWritesNothing() throws IOException {
        createModule(0, "alfa", "analista");
        String before = snapshot();

        Map<?, ?> out = addTechnician(0, "alfa", "desarrollador", "--dry-run");

        assertTrue(anyContains(data(out).get("created"), "desarrollador-alfa.md"));
        assertEquals(before, snapshot());
    }

    @Test
    void addTechnicianToAnUnknownModuleIsAnErrorAndWritesNothing() throws IOException {
        createModule(0, "alfa", "analista");
        String before = snapshot();

        Map<?, ?> out = addTechnician(1, "beta", "desarrollador");

        assertEquals("not_found.module", errorCode(out));
        assertEquals(before, snapshot());
    }

    @Test
    void invalidNamesAndValuesAreRejectedBeforeWritingAnything() throws IOException {
        String before = snapshot();

        assertEquals("validation.module.name_invalid_chars", errorCode(createModule(1, "modulo1", "analista")));
        assertEquals("validation.module.name_reserved", errorCode(createModule(1, "router", "analista")));
        assertEquals("usage.invalid_value", errorCode(createModule(2, "alfa", "jefe")));
        assertEquals("usage.invalid_value", errorCode(createModule(2, "alfa", "analista,")));
        assertEquals("validation.provider.unknown", errorCode(run(1, "create-module", "--project", project.toString(),
            "--module", "alfa", "--technicians", "analista", "--providers", "otro")));
        assertEquals("usage.missing_option", errorCode(run(2, "create-module", "--project", project.toString(),
            "--module", "alfa")));
        assertEquals(before, snapshot());
    }

    @Test
    void aProviderWithoutProjectSupportOnlyGivesAWarning() {
        Map<?, ?> out = run(0, "create-module", "--project", project.toString(), "--module", "alfa", "--technicians",
            "analista", "--providers", "codex");

        assertTrue(anyContains(data(out).get("warnings"), "no se soporta a nivel de proyecto"));
    }
}
