package es.vargontoc.agentic.develop.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

/** status, tree y describe de extremo a extremo: línea de comandos → servicios → envelope. */
class ReadCommandsTest {

    private static final JsonMapper JSON = JsonMapperFactory.create();
    private static final String AGENT = "---\nname: sdd-agent\n---\n\nEres el agente.\n";

    @TempDir
    Path tmp;

    Path home;
    Path project;
    CommandLine cli;

    private record Output(int exit, String raw) {
        Map<?, ?> json() {
            return JSON.readValue(raw, Map.class);
        }

        Map<?, ?> data() {
            return (Map<?, ?>) json().get("data");
        }

        Map<?, ?> firstError() {
            return (Map<?, ?>) ((List<?>) json().get("errors")).get(0);
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        home = Files.createDirectories(tmp.resolve("home"));
        project = Files.createDirectories(tmp.resolve("proyecto"));
        Services services = Services.of(new PathResolver(Map.of("HOME", home.toString()), PathResolver.Os.LINUX));
        cli = new CommandLine(Commands.standard(() -> services));
    }

    private Output run(String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int exit = cli.run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return new Output(exit, buffer.toString(StandardCharsets.UTF_8));
    }

    private Path file(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content);
    }

    private static String slashes(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static List<Path> snapshot(Path root) throws IOException {
        try(var files = Files.walk(root)) {
            return files.sorted().toList();
        }
    }

    // --- status ---

    @Test
    void statusReportsEveryProviderStateAndNoProjectByDefault() throws IOException {
        Files.createDirectories(home.resolve(".claude"));

        Output out = run("status");

        assertEquals(0, out.exit());
        assertEquals("status", out.json().get("command"));
        List<?> providers = (List<?>) out.data().get("providers");
        assertEquals(List.of("claude", "codex", "gemini", "opencode"),
            providers.stream().map(p -> ((Map<?, ?>) p).get("id")).toList());
        Map<?, ?> claude = (Map<?, ?>) providers.get(0);
        assertEquals("unconfigured", claude.get("state"));
        assertFalse(((List<?>) claude.get("missing")).isEmpty());
        assertEquals("not_installed", ((Map<?, ?>) providers.get(1)).get("state"));
        assertNull(out.data().get("project"));
    }

    @Test
    void statusIncludesTheProjectWhenGiven() throws IOException {
        file(project.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\n");

        Map<?, ?> info = (Map<?, ?>) run("status", "--project", project.toString()).data().get("project");

        assertEquals(slashes(project), info.get("path"));
        assertEquals(true, info.get("initialized"));
        assertEquals(List.of("alfa"), info.get("modules"));
    }

    @Test
    void statusOfAMissingProjectIsADomainErrorWithThePathAsTyped() {
        Path missing = tmp.resolve("no-existe");

        Output out = run("status", "--project", missing.toString());

        assertEquals(1, out.exit());
        assertEquals("not_found.project.path", out.firstError().get("code"));
        assertEquals(missing.toString(), out.firstError().get("path"));
    }

    // --- tree ---

    @Test
    void treeReturnsTheGlobalAndTheProjectTrees() throws IOException {
        file(home.resolve(".claude/agents/sdd-agent.md"), AGENT);
        file(project.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\n");
        file(project.resolve(".claude/agents/analista-router.md"), "r");
        file(project.resolve(".claude/agents/analista-alfa.md"), "a");

        Output out = run("tree", "--project", project.toString());

        assertEquals(0, out.exit());
        List<?> global = (List<?>) out.data().get("global");
        Map<?, ?> claude = (Map<?, ?>) global.get(0);
        assertEquals("provider", claude.get("type"));
        assertEquals("claude", claude.get("provider"));
        Map<?, ?> agent = (Map<?, ?>) ((List<?>) claude.get("children")).get(0);
        assertEquals(Map.of("type", "agent", "name", "sdd-agent", "status", "legacy"),
            Map.of("type", agent.get("type"), "name", agent.get("name"), "status", agent.get("status")));
        assertTrue(agent.get("path").toString().endsWith("/.claude/agents/sdd-agent.md"));

        List<?> projectNodes = (List<?>) out.data().get("project");
        assertEquals(List.of("sdd", "module", "provider"), projectNodes.stream().map(n -> ((Map<?, ?>) n).get("type")).toList());
        Map<?, ?> alfa = (Map<?, ?>) projectNodes.get(1);
        List<?> technicians = (List<?>) alfa.get("children");
        assertEquals(List.of(false, true, true), technicians.stream().map(t -> ((Map<?, ?>) t).get("pending")).toList());
    }

    @Test
    void treeWithoutProjectHasOnlyTheGlobalSide() {
        Output out = run("tree");

        assertEquals(0, out.exit());
        assertEquals(List.of(), out.data().get("project"));
        assertEquals(List.of(), out.data().get("warnings"));
    }

    @Test
    void treeReportsTeamWarningsAndUnreadableArtifactsWithoutFailing() throws IOException {
        file(project.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\nbasura\n");
        Path broken = file(project.resolve(".claude/agents/analista-alfa.md"), "x");
        Files.write(broken, new byte[] {(byte) 0xC3, (byte) 0x28});

        Output out = run("tree", "--project", project.toString());

        assertEquals(0, out.exit());
        Map<?, ?> warning = (Map<?, ?>) ((List<?>) out.data().get("warnings")).get(0);
        assertEquals(Map.of("type", "malformed_line", "line", 4), warning);
        Map<?, ?> problem = (Map<?, ?>) ((List<?>) out.data().get("problems")).get(0);
        assertEquals(slashes(broken), problem.get("path"));
        assertEquals("internal.text.read", problem.get("code"));
    }

    // --- describe ---

    @Test
    void describeReturnsHeaderBodyHashAndStatusOfAnAgent() throws IOException {
        Path agent = file(home.resolve(".claude/agents/sdd-agent.md"), AGENT);

        Output out = run("describe", "--path", agent.toString());

        assertEquals(0, out.exit());
        assertEquals("describe", out.json().get("command"));
        Map<?, ?> d = out.data();
        assertEquals("claude", d.get("provider"));
        assertEquals("global", d.get("scope"));
        assertEquals("agent", d.get("kind"));
        assertEquals("markdown", d.get("format"));
        assertEquals("---\nname: sdd-agent\n---\n\n", d.get("header"));
        assertEquals("Eres el agente.\n", d.get("body"));
        assertEquals("legacy", d.get("status"));
        assertEquals(false, d.get("modifiedExternally"));
        assertEquals(64, d.get("hash").toString().length());
        assertNull(d.get("sharedBody"));
        assertNull(d.get("module"));
    }

    @Test
    void describeOfAProjectSpecialistNeedsTheProjectAndExposesItsModule() throws IOException {
        file(project.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\n");
        Path agent = file(project.resolve(".claude/agents/analista-alfa.md"), AGENT);

        Output without = run("describe", "--path", agent.toString());
        Output with = run("describe", "--path", agent.toString(), "--project", project.toString());

        assertEquals(1, without.exit());
        assertEquals("validation.artifact.not_catalogued", without.firstError().get("code"));
        assertEquals(0, with.exit());
        assertEquals("alfa", with.data().get("module"));
        assertEquals("project", with.data().get("scope"));
    }

    @Test
    void describeErrorsCarryTheirPathAndExitWithOne() throws IOException {
        Path missing = home.resolve(".claude/agents/sdd-agent.md");

        Output notFound = run("describe", "--path", missing.toString());
        Output inJar = run("describe", "--path", "/global/agents/claude/sdd-agent.md");

        assertEquals(1, notFound.exit());
        assertEquals("not_found.artifact.path", notFound.firstError().get("code"));
        assertEquals(missing.toString(), notFound.firstError().get("path"));
        assertEquals(1, inJar.exit());
        assertEquals("validation.artifact.not_editable", inJar.firstError().get("code"));
    }

    @Test
    void describeWithoutPathIsAUsageError() {
        Output out = run("describe");

        assertEquals(2, out.exit());
        assertEquals("usage.missing_option", out.firstError().get("code"));
    }

    // --- transversales ---

    @Test
    void helpListsTheThreeReadCommands() {
        Output out = run("--help");

        List<?> commands = (List<?>) out.data().get("commands");
        assertEquals(List.of("add-technician", "configure-provider", "create-artifact", "create-module", "describe", "init", "save", "status", "tree"),commands.stream().map(c -> ((Map<?, ?>) c).get("name")).toList());
    }

    @Test
    void readCommandsNeverWriteAnything() throws IOException {
        file(home.resolve(".claude/agents/sdd-agent.md"), AGENT);
        file(project.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\n");
        Path agent = file(project.resolve(".claude/agents/analista-alfa.md"), AGENT);
        List<Path> before = snapshot(tmp);

        run("status", "--project", project.toString());
        run("tree", "--project", project.toString());
        run("describe", "--path", agent.toString(), "--project", project.toString());
        run("describe", "--path", home.resolve(".claude/agents/sdd-agent.md").toString());

        assertEquals(before, snapshot(tmp));
    }
}
