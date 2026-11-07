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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

/** save de extremo a extremo con un home y un proyecto temporales. */
class SaveCommandTest {

    private static final JsonMapper JSON = JsonMapperFactory.create();

    @TempDir
    Path tmp;

    Path home;
    Path project;
    Services services;
    CommandLine cli;
    byte[] stdin = new byte[0];

    @BeforeEach
    void setUp() throws IOException {
        home = Files.createDirectories(tmp.resolve("home"));
        project = Files.createDirectories(tmp.resolve("proyecto"));
        services = Services.of(new PathResolver(Map.of("HOME", home.toString()), PathResolver.Os.LINUX));
        cli = new CommandLine(Commands.standard(() -> services, () -> (InputStream) new ByteArrayInputStream(stdin)));
        Map<?, ?> configured = run(0, "configure-provider", "--provider", "claude");
        // Los avisos del hook dependen del sistema de ficheros del equipo de pruebas (permiso de ejecución en Linux)
        // y no tienen que ver con lo que este test comprueba
        List<?> warnings = (List<?>) data(configured).get("warnings");
        assertEquals(List.of(), warnings.stream().filter(w -> !String.valueOf(w).startsWith("hooks/")).toList());
    }

    private Map<?, ?> run(int expectedExit, String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        assertEquals(expectedExit, cli.run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8)));
        return JSON.readValue(buffer.toString(StandardCharsets.UTF_8), Map.class);
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

    private String body(Path agent) throws IOException {
        return ArtifactFormat.MARKDOWN.split(Files.readString(agent)).body();
    }

    private void twoModules() {
        List<Provider> claude = List.of(new Provider(ProviderType.CLAUDE, Path.of("x")));
        services.workspace().initialize(project);
        services.createModule().execute(project, "alfa", List.of(Technician.ANALYST), claude);
        services.createModule().execute(project, "beta", List.of(Technician.ANALYST), claude);
    }

    @Test
    void savingAnInstanceWritesTheBodyReturnsTheDiffAndKeepsTheHeader() throws IOException {
        String header = ArtifactFormat.MARKDOWN.split(Files.readString(agent())).header();

        Map<?, ?> out = run(0, "save", "--path", agent().toString(), "--content-file", content("Cuerpo nuevo\n").toString());

        assertEquals("save", out.get("command"));
        assertEquals("instance", data(out).get("mode"));
        assertEquals(true, data(out).get("written"));
        assertEquals(true, data(out).get("tracked"));
        assertTrue(((String) data(out).get("diff")).contains("+Cuerpo nuevo"));
        assertEquals("Cuerpo nuevo\n", body(agent()));
        assertEquals(header, ArtifactFormat.MARKDOWN.split(Files.readString(agent())).header());
    }

    @Test
    void savingTheSameContentAgainWritesNothing() throws IOException {
        Path file = content("Cuerpo nuevo\n");
        run(0, "save", "--path", agent().toString(), "--content-file", file.toString());
        String before = Files.readString(agent());

        Map<?, ?> out = run(0, "save", "--path", agent().toString(), "--content-file", file.toString());

        assertEquals(false, data(out).get("written"));
        assertTrue(((List<?>) data(out).get("backups")).isEmpty());
        assertEquals(before, Files.readString(agent()));
    }

    @Test
    void contentCanComeFromStandardInputOnlyWhenAsked() throws IOException {
        stdin = "Desde stdin\n".getBytes(StandardCharsets.UTF_8);

        run(0, "save", "--path", agent().toString(), "--stdin");

        assertEquals("Desde stdin\n", body(agent()));
    }

    @Test
    void anExternalChangeIsAConflictUnlessOverwriteIsGivenAndThenABackupRemains() throws IOException {
        Files.writeString(agent(), Files.readString(agent()) + "\nretocado por otra herramienta\n");
        String external = Files.readString(agent());
        Path file = content("Cuerpo propio\n");

        Map<?, ?> conflict = run(1, "save", "--path", agent().toString(), "--content-file", file.toString());
        assertEquals("conflict.artifact.modified_externally", errorCode(conflict));
        assertEquals(external, Files.readString(agent()));

        Map<?, ?> forced = run(0, "save", "--path", agent().toString(), "--content-file", file.toString(), "--overwrite");
        assertEquals(true, data(forced).get("written"));
        List<?> backups = (List<?>) data(forced).get("backups");
        assertEquals(1, backups.size());
        assertTrue(Files.readString(Path.of((String) backups.get(0))).contains("retocado por otra herramienta"));
    }

    @Test
    void groupModeUpdatesEveryModuleKeepingEachModuleNameAndCreatesNothing() throws IOException {
        twoModules();
        Path alfa = project.resolve(".claude/agents/analista-alfa.md");
        Path beta = project.resolve(".claude/agents/analista-beta.md");
        Files.delete(beta);
        services.createModule().execute(project, "gamma", List.of(Technician.ANALYST), List.of());

        Map<?, ?> out = run(0, "save", "--path", alfa.toString(), "--project", project.toString(), "--mode", "group",
            "--content-file", content("Soy el analista de {module}\n").toString());

        assertEquals("group", data(out).get("mode"));
        assertEquals("project/specialist/analista", data(out).get("groupId"));
        assertEquals("Soy el analista de alfa\n", body(alfa));
        assertFalse(Files.exists(beta));
        assertFalse(Files.exists(project.resolve(".claude/agents/analista-gamma.md")));
        List<?> members = (List<?>) data(out).get("members");
        assertTrue(members.stream().anyMatch(m -> "updated".equals(((Map<?, ?>) m).get("outcome"))));
        assertTrue(members.stream().anyMatch(m -> "missing".equals(((Map<?, ?>) m).get("outcome"))));
    }

    @Test
    void groupModeSkipsConflictingMembersAndReportsThem() throws IOException {
        twoModules();
        Path alfa = project.resolve(".claude/agents/analista-alfa.md");
        Path beta = project.resolve(".claude/agents/analista-beta.md");
        Files.writeString(beta, Files.readString(beta) + "\nexterno\n");
        String externalBeta = Files.readString(beta);

        Map<?, ?> out = run(0, "save", "--path", alfa.toString(), "--project", project.toString(), "--mode", "group",
            "--content-file", content("Nuevo {module}\n").toString());

        assertEquals(externalBeta, Files.readString(beta));
        assertTrue(((List<?>) data(out).get("members")).stream()
            .anyMatch(m -> "conflict".equals(((Map<?, ?>) m).get("outcome"))));
    }

    @Test
    void aSkillFileIsSavedAndTheRestOfTheSkillIsKept() throws IOException {
        Path skill = home.resolve(".claude/skills/sdd-design");
        Path other = skill.resolve("agents");
        Files.createDirectories(other);
        Files.writeString(other.resolve("extra.txt"), "auxiliar");

        // El fichero auxiliar añadido a mano cuenta como cambio externo de la skill, de ahí --overwrite
        Map<?, ?> out = run(0, "save", "--path", skill.toString(), "--content-file", content("# Nueva\n").toString(),
            "--overwrite");

        assertEquals(true, data(out).get("written"));
        assertTrue(((String) data(out).get("diff")).startsWith("@@ SKILL.md"));
        assertEquals("# Nueva\n", Files.readString(skill.resolve("SKILL.md")).replace("\r\n", "\n"));
        assertEquals("auxiliar", Files.readString(other.resolve("extra.txt")));
    }

    @Test
    void aPathOutsideTheAppAndAMissingContentFileAreErrors() throws IOException {
        Path foreign = Files.writeString(home.resolve(".claude/agents/ajeno.md"), "mio");

        Map<?, ?> outside = run(1, "save", "--path", foreign.toString(), "--content-file", content("x").toString());
        assertEquals("validation.artifact.not_catalogued", errorCode(outside));
        assertEquals("mio", Files.readString(foreign));

        Map<?, ?> missing = run(1, "save", "--path", agent().toString(), "--content-file", tmp.resolve("nada.md").toString());
        assertEquals("validation.content.unreadable", errorCode(missing));
    }

    @Test
    void usageErrorsDoNotTouchAnything() throws IOException {
        String before = Files.readString(agent());

        assertEquals("usage.missing_option", errorCode(run(2, "save", "--path", agent().toString())));
        assertEquals("usage.invalid_value", errorCode(run(2, "save", "--path", agent().toString(), "--mode", "todos",
            "--content-file", content("x").toString())));
        assertEquals("usage.unexpected_argument", errorCode(run(2, "save", "--path", agent().toString(), "--stdin",
            "--content-file", content("x").toString())));
        assertEquals(before, Files.readString(agent()));
        assertNotNull(before);
    }
}
