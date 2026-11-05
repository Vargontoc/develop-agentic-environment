package es.vargontoc.agentic.develop.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

/** Lo que el visor necesita para editar: catálogo y plantilla, cabecera, simulación de guardado y adopción. */
class ViewerSupportCommandsTest {

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
        freshCli();
    }

    // El almacén de metadatos guarda una copia en memoria por instancia: tras tocarlo desde fuera hay que reconstruir
    private Services freshCli() {
        Services services = Services.of(new PathResolver(Map.of("HOME", home.toString()), PathResolver.Os.LINUX));
        cli = new CommandLine(Commands.standard(() -> services, () -> (InputStream) new ByteArrayInputStream(stdin)));
        return services;
    }

    private Map<?, ?> run(int expectedExit, String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        assertEquals(expectedExit, cli.run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8)), String.join(" ", args));
        return JSON.readValue(buffer.toString(StandardCharsets.UTF_8), Map.class);
    }

    private static Map<?, ?> data(Map<?, ?> envelope) {
        return (Map<?, ?>) envelope.get("data");
    }

    private static String errorCode(Map<?, ?> envelope) {
        return (String) ((Map<?, ?>) ((List<?>) envelope.get("errors")).get(0)).get("code");
    }

    private static List<?> list(Object value) {
        return (List<?>) value;
    }

    private Path file(String text) throws IOException {
        return Files.writeString(tmp.resolve("f-" + System.nanoTime() + ".txt"), text);
    }

    private Path agent(String name) {
        return home.resolve(".claude/agents/" + name + ".md");
    }

    private String snapshot() throws IOException {
        try(Stream<Path> files = Files.walk(tmp)) {
            return files.filter(Files::isRegularFile).map(f -> tmp.relativize(f) + ":" + f.toFile().length())
                .sorted().toList().toString();
        }
    }

    // ---- catalog ----

    @Test
    void catalogListsTheEntriesAndWhichOnesAlreadyExistWithoutWritingAnything() throws IOException {
        run(0, "create-artifact", "--provider", "claude", "--scope", "global", "--kind", "agent", "--name", "sdd-design");
        String before = snapshot();

        Map<?, ?> out = run(0, "catalog", "--provider", "claude", "--scope", "global", "--kind", "agent");

        assertEquals("catalog", out.get("command"));
        List<?> entries = list(data(out).get("entries"));
        assertTrue(entries.size() >= 2);
        Map<?, ?> design = (Map<?, ?>) entries.stream().filter(e -> "sdd-design".equals(((Map<?, ?>) e).get("name"))).findFirst().orElseThrow();
        Map<?, ?> tasklist = (Map<?, ?>) entries.stream().filter(e -> "sdd-tasklist".equals(((Map<?, ?>) e).get("name"))).findFirst().orElseThrow();
        assertEquals(true, design.get("exists"));
        assertEquals(false, tasklist.get("exists"));
        assertNull(design.get("template"));
        assertEquals("agents/sdd-design.md", design.get("relativePath"));
        assertEquals(before, snapshot());
    }

    @Test
    void catalogByNameReturnsTheComposedTemplateThatCreateArtifactWouldWrite() throws IOException {
        Map<?, ?> out = run(0, "catalog", "--provider", "claude", "--scope", "global", "--kind", "agent", "--name", "sdd-design");

        Map<?, ?> entry = (Map<?, ?>) list(data(out).get("entries")).get(0);
        Map<?, ?> template = (Map<?, ?>) entry.get("template");
        assertFalse(Files.exists(agent("sdd-design")));
        assertTrue(((String) template.get("header")).contains("name: sdd-design"));
        assertTrue(((String) template.get("header")).contains("shell: bash") || ((String) template.get("header")).contains("shell: powershell"));

        run(0, "create-artifact", "--provider", "claude", "--scope", "global", "--kind", "agent", "--name", "sdd-design");
        assertEquals(Files.readString(agent("sdd-design")).replace("\r\n", "\n"),
            ((String) template.get("content")).replace("\r\n", "\n"));
        assertEquals(template.get("body"), ArtifactFormat.MARKDOWN.split(Files.readString(agent("sdd-design"))).body());
    }

    @Test
    void catalogOfASkillReturnsItsFilesAndOfAProjectSpecialistTheResolvedModule() throws IOException {
        Map<?, ?> skill = run(0, "catalog", "--provider", "codex", "--scope", "global", "--kind", "skill", "--name", "sdd-design");
        List<?> files = list(((Map<?, ?>) ((Map<?, ?>) list(data(skill).get("entries")).get(0)).get("template")).get("files"));
        assertTrue(files.stream().anyMatch(f -> "SKILL.md".equals(((Map<?, ?>) f).get("path"))));
        assertTrue(files.stream().anyMatch(f -> "agents/openai.yaml".equals(((Map<?, ?>) f).get("path"))));

        run(0, "init", "--project", project.toString());
        run(0, "create-module", "--project", project.toString(), "--module", "alfa", "--technicians", "analista", "--providers", "claude");
        Map<?, ?> specialist = run(0, "catalog", "--provider", "claude", "--scope", "project", "--kind", "agent", "--name",
            "analista-alfa", "--project", project.toString());
        Map<?, ?> entry = (Map<?, ?>) list(data(specialist).get("entries")).get(0);
        assertEquals("alfa", entry.get("module"));
        assertEquals("analista", entry.get("technician"));
        assertEquals(true, entry.get("exists"));
        assertFalse(((String) ((Map<?, ?>) entry.get("template")).get("content")).contains("{module}"));
    }

    @Test
    void catalogErrors() {
        assertEquals("validation.artifact.not_catalogued",
            errorCode(run(1, "catalog", "--provider", "claude", "--scope", "global", "--kind", "agent", "--name", "no-existe")));
        assertEquals("validation.artifact.scope_unsupported",
            errorCode(run(1, "catalog", "--provider", "codex", "--scope", "project", "--kind", "agent", "--project", project.toString())));
        assertEquals("validation.provider.unknown",
            errorCode(run(1, "catalog", "--provider", "otro", "--scope", "global", "--kind", "agent")));
        assertEquals("usage.invalid_value",
            errorCode(run(2, "catalog", "--provider", "claude", "--scope", "global", "--kind", "perfil")));
    }

    // ---- save --header ----

    @Test
    void aHeaderCanBeReplacedKeepingTheBodyAndTheHashStaysInSync() throws IOException {
        run(0, "configure-provider", "--provider", "claude");
        var before = ArtifactFormat.MARKDOWN.split(Files.readString(agent("sdd-design")));
        String header = before.header().replace("name: sdd-design", "name: sdd-design\ndescription-extra: probando");

        Map<?, ?> out = run(0, "save", "--path", agent("sdd-design").toString(), "--header-file", file(header).toString());

        assertEquals(true, data(out).get("written"));
        assertEquals(true, data(out).get("tracked"));
        var after = ArtifactFormat.MARKDOWN.split(Files.readString(agent("sdd-design")));
        assertTrue(after.header().contains("description-extra: probando"));
        assertEquals(before.body(), after.body());
        // El registro sigue al contenido: describe no ve cambios externos y un guardado normal no da conflicto
        assertEquals(false, data(run(0, "describe", "--path", agent("sdd-design").toString())).get("modifiedExternally"));
        run(0, "save", "--path", agent("sdd-design").toString(), "--content-file", file("Otro cuerpo\n").toString());
    }

    @Test
    void headerAndBodyCanBeSavedTogetherAndHeaderCanComeFromStandardInput() throws IOException {
        run(0, "configure-provider", "--provider", "claude");
        String header = ArtifactFormat.MARKDOWN.split(Files.readString(agent("sdd-design"))).header();
        stdin = (header + "\n").getBytes(StandardCharsets.UTF_8);

        run(0, "save", "--path", agent("sdd-design").toString(), "--header-stdin", "--content-file", file("Cuerpo nuevo\n").toString());

        assertEquals("Cuerpo nuevo\n", ArtifactFormat.MARKDOWN.split(Files.readString(agent("sdd-design"))).body());
    }

    @Test
    void anInvalidHeaderIsRejectedAndNothingIsWritten() throws IOException {
        run(0, "configure-provider", "--provider", "claude");
        String before = Files.readString(agent("sdd-design"));

        // Sin cierre del frontmatter: al volver a leerlo se tragaría el cuerpo
        Map<?, ?> broken = run(1, "save", "--path", agent("sdd-design").toString(), "--header-file", file("---\nname: x\n").toString());
        assertEquals("validation.artifact.format_invalid", errorCode(broken));
        Map<?, ?> none = run(1, "save", "--path", agent("sdd-design").toString(), "--header-file", file("sin frontmatter").toString());
        assertEquals("validation.artifact.format_invalid", errorCode(none));
        assertEquals(before, Files.readString(agent("sdd-design")));
    }

    @Test
    void aHeaderOnlyMakesSenseForOneAgentInstance() throws IOException {
        run(0, "configure-provider", "--provider", "claude");
        Path header = file("---\nname: x\n---\n");

        assertEquals("usage.unexpected_argument", errorCode(run(2, "save", "--path", agent("sdd-design").toString(),
            "--mode", "group", "--content-file", file("x").toString(), "--header-file", header.toString())));
        assertEquals("usage.unexpected_argument", errorCode(run(2, "save", "--path", home.resolve(".claude/skills/sdd-design").toString(),
            "--content-file", file("x").toString(), "--header-file", header.toString())));
        assertEquals("usage.unexpected_argument", errorCode(run(2, "save", "--path", agent("sdd-design").toString(),
            "--stdin", "--header-stdin")));
    }

    // ---- save --dry-run ----

    @Test
    void dryRunOfAnInstanceReturnsTheDiffWithoutWriting() throws IOException {
        run(0, "configure-provider", "--provider", "claude");
        String before = Files.readString(agent("sdd-design"));
        Path content = file("Cuerpo simulado\n");
        String snapshot = snapshot();

        Map<?, ?> out = run(0, "save", "--path", agent("sdd-design").toString(), "--content-file", content.toString(), "--dry-run");

        assertEquals(true, data(out).get("dryRun"));
        assertEquals(true, data(out).get("written"));
        assertTrue(((String) data(out).get("diff")).contains("+Cuerpo simulado"));
        assertTrue(list(data(out).get("backups")).isEmpty());
        assertEquals(before, Files.readString(agent("sdd-design")));
        assertEquals(snapshot, snapshot());

        // Lo previsto coincide con lo que luego se escribe
        Map<?, ?> real = run(0, "save", "--path", agent("sdd-design").toString(), "--content-file", content.toString());
        assertEquals(data(out).get("diff"), data(real).get("diff"));
    }

    @Test
    void dryRunOfAGroupReturnsMembersDiffsAndConflictsWithoutWriting() throws IOException {
        run(0, "init", "--project", project.toString());
        run(0, "create-module", "--project", project.toString(), "--module", "alfa", "--technicians", "analista", "--providers", "claude");
        run(0, "create-module", "--project", project.toString(), "--module", "beta", "--technicians", "analista", "--providers", "claude");
        Path alfa = project.resolve(".claude/agents/analista-alfa.md");
        Path beta = project.resolve(".claude/agents/analista-beta.md");
        Files.writeString(beta, Files.readString(beta) + "\nexterno\n");
        Path content = file("Analista de {module}\n");
        String snapshot = snapshot();

        Map<?, ?> out = run(0, "save", "--path", alfa.toString(), "--project", project.toString(), "--mode", "group",
            "--content-file", content.toString(), "--dry-run");

        assertEquals(true, data(out).get("dryRun"));
        List<?> members = list(data(out).get("members"));
        Map<?, ?> updated = (Map<?, ?>) members.stream().filter(m -> "updated".equals(((Map<?, ?>) m).get("outcome"))).findFirst().orElseThrow();
        assertTrue(((String) updated.get("diff")).contains("+Analista de alfa"));
        assertTrue(members.stream().anyMatch(m -> "conflict".equals(((Map<?, ?>) m).get("outcome"))));
        assertEquals(snapshot, snapshot());

        // El cuerpo compartido del grupo tampoco cambió: un guardado real posterior sigue viéndolo como cambio
        Map<?, ?> real = run(0, "save", "--path", alfa.toString(), "--project", project.toString(), "--mode", "group",
            "--content-file", content.toString());
        assertEquals(true, data(real).get("groupUpdated"));
        assertEquals("Analista de alfa\n", ArtifactFormat.MARKDOWN.split(Files.readString(alfa)).body());
    }

    @Test
    void dryRunOfASkillAndOfAConflictingInstance() throws IOException {
        run(0, "configure-provider", "--provider", "claude");
        Path skill = home.resolve(".claude/skills/sdd-design");
        Path content = file("# Simulada\n");
        String snapshot = snapshot();

        Map<?, ?> out = run(0, "save", "--path", skill.toString(), "--content-file", content.toString(), "--dry-run");
        assertEquals(true, data(out).get("written"));
        assertTrue(((String) data(out).get("diff")).startsWith("@@ SKILL.md"));
        assertEquals(snapshot, snapshot());

        Files.writeString(agent("sdd-design"), Files.readString(agent("sdd-design")) + "\nexterno\n");
        Map<?, ?> conflict = run(1, "save", "--path", agent("sdd-design").toString(), "--content-file", file("x\n").toString(), "--dry-run");
        assertEquals("conflict.artifact.modified_externally", errorCode(conflict));
    }

    // ---- adopt ----

    private void legacyAgent() throws IOException {
        // Un agente de proyecto generado antes de existir los metadatos: sin registro
        run(0, "init", "--project", project.toString());
        run(0, "create-module", "--project", project.toString(), "--module", "alfa", "--technicians", "analista", "--providers", "claude");
        run(0, "create-module", "--project", project.toString(), "--module", "beta", "--technicians", "analista", "--providers", "claude");
        run(0, "save", "--path", project.resolve(".claude/agents/analista-alfa.md").toString(), "--project", project.toString(),
            "--mode", "group", "--content-file", file("Cuerpo del grupo para {module}\n").toString());
        Path store = Path.of(home.toString(), ".local/state/agentic-develop/metadata.json");
        String content = Files.readString(store);
        String key = project.resolve(".claude/agents/analista-beta.md").toAbsolutePath().normalize().toString().replace('\\', '/');
        // Se quita el registro de beta a mano para simular un artefacto legacy
        var snapshot = JSON.readValue(content, Map.class);
        ((Map<?, ?>) snapshot.get("artifacts")).remove(key);
        Files.writeString(store, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot));
        Files.writeString(project.resolve(".claude/agents/analista-beta.md"),
            Files.readString(project.resolve(".claude/agents/analista-beta.md")).replace("Cuerpo del grupo para beta", "Mi version personal"));
        freshCli();
    }

    @Test
    void adoptPreviewsByDefaultAndAppliesOnlyWithConfirm() throws IOException {
        legacyAgent();
        Path beta = project.resolve(".claude/agents/analista-beta.md");
        String before = Files.readString(beta);
        String[] base = {"adopt", "--path", beta.toString(), "--project", project.toString()};

        Map<?, ?> preview = run(0, base);
        assertEquals(true, data(preview).get("dryRun"));
        assertEquals(false, data(preview).get("applied"));
        assertEquals("project/specialist/analista", data(preview).get("groupId"));
        assertTrue(((String) data(preview).get("diff")).contains("+Cuerpo del grupo para beta"));
        assertEquals(before, Files.readString(beta));

        Map<?, ?> explicit = run(0, "adopt", "--path", beta.toString(), "--project", project.toString(), "--dry-run");
        assertEquals(false, data(explicit).get("applied"));
        assertEquals(before, Files.readString(beta));

        Map<?, ?> applied = run(0, "adopt", "--path", beta.toString(), "--project", project.toString(), "--confirm");
        assertEquals(true, data(applied).get("applied"));
        assertEquals(false, data(applied).get("dryRun"));
        assertNotNull(data(applied).get("backup"));
        assertTrue(Files.readString(Path.of((String) data(applied).get("backup"))).contains("Mi version personal"));
        assertEquals("Cuerpo del grupo para beta\n", ArtifactFormat.MARKDOWN.split(Files.readString(beta)).body());
        assertEquals("linked", data(run(0, "describe", "--path", beta.toString(), "--project", project.toString())).get("status"));
    }

    @Test
    void aDetachedArtifactCanComeBackToItsGroup() throws IOException {
        legacyAgent();
        Path alfa = project.resolve(".claude/agents/analista-alfa.md");
        // Desvincular: se marca en el almacén como hace la aplicación
        var services = freshCli();
        var location = services.descriptor().resolve(alfa.toString(), project);
        assertTrue(services.migration().detach(location));
        freshCli();
        Files.writeString(alfa, Files.readString(alfa).replace("Cuerpo del grupo para alfa", "Divergente"));
        assertEquals("detached", data(run(0, "describe", "--path", alfa.toString(), "--project", project.toString())).get("status"));

        Map<?, ?> back = run(0, "adopt", "--path", alfa.toString(), "--project", project.toString(), "--confirm");

        assertEquals(true, data(back).get("applied"));
        assertEquals("Cuerpo del grupo para alfa\n", ArtifactFormat.MARKDOWN.split(Files.readString(alfa)).body());
        assertEquals("linked", data(run(0, "describe", "--path", alfa.toString(), "--project", project.toString())).get("status"));
    }

    @Test
    void adoptingALinkedArtifactOrWithUnknownGroupOrBadOptionsIsAnError() throws IOException {
        legacyAgent();
        Path alfa = project.resolve(".claude/agents/analista-alfa.md");
        Path beta = project.resolve(".claude/agents/analista-beta.md");

        assertEquals("conflict.artifact.already_tracked",
            errorCode(run(1, "adopt", "--path", alfa.toString(), "--project", project.toString(), "--confirm")));
        assertEquals("not_found.group",
            errorCode(run(1, "adopt", "--path", beta.toString(), "--project", project.toString(), "--group", "project/specialist/inexistente")));
        assertEquals("usage.unexpected_argument",
            errorCode(run(2, "adopt", "--path", beta.toString(), "--project", project.toString(), "--confirm", "--dry-run")));
        assertEquals("usage.unexpected_argument",
            errorCode(run(2, "adopt", "--path", beta.toString(), "--project", project.toString(), "--content-file", file("x").toString())));
    }

    @Test
    void aLegacySkillIsLinkedAsIs() throws IOException {
        // Skill instalada a mano (sin registro)
        Path skill = Files.createDirectories(home.resolve(".claude/skills/sdd-design"));
        Files.writeString(skill.resolve("SKILL.md"), "# Mi skill\n");

        Map<?, ?> preview = run(0, "adopt", "--path", skill.toString());
        assertEquals(false, data(preview).get("applied"));
        assertEquals("global/skill/sdd-design", data(preview).get("groupId"));

        Map<?, ?> applied = run(0, "adopt", "--path", skill.toString(), "--confirm");
        assertEquals("# Mi skill\n", Files.readString(skill.resolve("SKILL.md")));
        assertEquals("linked", data(run(0, "describe", "--path", skill.toString())).get("status"));
        assertNotNull(applied);
        assertTrue(Files.isRegularFile(Path.of(home.toString(), ".local/state/agentic-develop/metadata.json")));
        assertNotNull(MetadataStore.key(skill));
    }
}
