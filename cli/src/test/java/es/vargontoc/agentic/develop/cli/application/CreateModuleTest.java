package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
import es.vargontoc.agentic.develop.cli.domain.metadata.ContentHash;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;
import es.vargontoc.agentic.develop.cli.support.Console;

class CreateModuleTest {

    @TempDir
    Path tmp;

    Path project;
    PathResolver paths;
    MetadataStore metadata;
    CreateModule createModule;
    final List<Provider> claude = List.of(new Provider(ProviderType.CLAUDE, Path.of("ignorado")));

    @BeforeEach
    void setUp() throws IOException {
        project = Files.createDirectories(tmp.resolve("proyecto"));
        paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        Path backups = tmp.resolve("backups");
        new ProjectWorkspace(paths, backups).initialize(project);

        var catalog = new ResourceArtifactCatalog();
        metadata = new MetadataStore(tmp.resolve("app-home/metadata.json"), new TextStore(paths, List.of(tmp), backups));
        var inventory = new ArtifactInventory(paths, catalog, metadata, backups);
        createModule = new CreateModule(TargetOs.LINUX, paths, catalog, inventory, metadata, backups);
    }

    private static void assertInvalid(AppErrorCode expected, String name, CreateModule createModule) {
        DomainException e = assertThrows(DomainException.class, () -> createModule.normalizeName(name), name);
        assertEquals(expected, e.code(), name);
    }

    @Test
    void validNamesAreNormalizedToLowercase() {
        assertEquals("mi-modulo", createModule.normalizeName("  Mi-Modulo "));
        assertEquals("abc", createModule.normalizeName("ABC"));
    }

    @Test
    void eachNameRuleHasItsOwnErrorCode() {
        assertInvalid(AppErrorCode.MODULE_NAME_INVALID_CHARS, "modulo1", createModule);
        assertInvalid(AppErrorCode.MODULE_NAME_INVALID_CHARS, "mi modulo", createModule);
        assertInvalid(AppErrorCode.MODULE_NAME_INVALID_CHARS, "módulo", createModule);
        assertInvalid(AppErrorCode.MODULE_NAME_INVALID_HYPHENS, "-abc", createModule);
        assertInvalid(AppErrorCode.MODULE_NAME_INVALID_HYPHENS, "abc-", createModule);
        assertInvalid(AppErrorCode.MODULE_NAME_INVALID_HYPHENS, "a--b", createModule);
        assertInvalid(AppErrorCode.MODULE_NAME_TOO_SHORT, "ab", createModule);
        assertInvalid(AppErrorCode.MODULE_NAME_RESERVED, "router", createModule);
        assertInvalid(AppErrorCode.MODULE_NAME_RESERVED, "Router", createModule);
    }

    @Test
    void creatingAModuleReturnsWhatWasCreatedAndPrintsNothing() throws IOException {
        Console.Captured<OperationResult> run = Console.capture(() ->
            createModule.execute(project, "alfa", List.of(Technician.ANALYST, Technician.DEVELOPER), claude));

        assertEquals("", run.out());
        OperationResult result = run.value();
        assertTrue(result.created().stream().anyMatch(c -> c.contains("analista-router.md")));
        assertTrue(result.created().stream().anyMatch(c -> c.contains("analista-alfa.md")));
        assertTrue(result.created().stream().anyMatch(c -> c.contains("desarrollador-alfa.md")));
        assertTrue(result.created().stream().anyMatch(c -> c.contains("team.md")));
        assertTrue(Files.isRegularFile(project.resolve(".claude/agents/analista-alfa.md")));
        assertTrue(Files.readString(project.resolve(".sdd/team.md")).contains("- alfa | - | -"));
        assertFalse(Files.readString(project.resolve(".claude/agents/analista-alfa.md")).contains("{module}"));
    }

    @Test
    void repeatingTheSameCreationDuplicatesAndOverwritesNothing() throws IOException {
        List<Technician> technicians = List.of(Technician.ANALYST);
        createModule.execute(project, "alfa", technicians, claude);
        Path agent = project.resolve(".claude/agents/analista-alfa.md");
        Files.writeString(agent, "personalizado");

        OperationResult again = createModule.execute(project, "alfa", technicians, claude);

        assertTrue(again.created().isEmpty());
        assertTrue(again.skipped().stream().anyMatch(s -> s.contains("analista-alfa.md")));
        assertEquals("personalizado", Files.readString(agent));
        long registrations = Files.readAllLines(project.resolve(".sdd/team.md")).stream().filter(l -> l.startsWith("- alfa")).count();
        assertEquals(1, registrations);
    }

    @Test
    void addingAnotherTechnicianToAnExistingModuleOnlyCreatesTheNewOne() throws IOException {
        createModule.execute(project, "alfa", List.of(Technician.ANALYST), claude);
        Path analyst = project.resolve(".claude/agents/analista-alfa.md");
        String before = Files.readString(analyst);

        OperationResult result = createModule.execute(project, "alfa", List.of(Technician.ANALYST, Technician.DEVELOPER), claude);

        assertTrue(result.created().stream().anyMatch(c -> c.contains("desarrollador-alfa.md")));
        assertFalse(result.created().stream().anyMatch(c -> c.contains("analista-alfa.md")));
        assertEquals(before, Files.readString(analyst));
    }

    @Test
    void providersWithoutProjectSupportAreSkippedWithAWarning() {
        OperationResult result = createModule.execute(project, "alfa", List.of(Technician.ANALYST),
            List.of(new Provider(ProviderType.CODEX, Path.of("ignorado"))));

        assertEquals(1, result.warnings().stream().filter(w -> w.contains("Codex")).count());
        assertFalse(Files.exists(project.resolve(".codex")));
    }

    @Test
    void providerWithoutProjectHeadersGivesWarningsAndCreatesNoAgents() {
        OperationResult result = createModule.execute(project, "alfa", List.of(Technician.ANALYST),
            List.of(new Provider(ProviderType.OPENCODE, Path.of("ignorado"))));

        assertEquals(2, result.warnings().stream().filter(w -> w.contains("no existe el recurso")).count());
        assertFalse(Files.exists(project.resolve(".opencode/agents/analista-alfa.md")));
    }

    @Test
    void missingTeamFileOnlyWarns() throws IOException {
        Files.delete(project.resolve(".sdd/team.md"));

        OperationResult result = createModule.execute(project, "alfa", List.of(Technician.ANALYST), claude);

        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("team.md")));
        assertTrue(Files.isRegularFile(project.resolve(".claude/agents/analista-alfa.md")));
    }

    @Test
    void generatedArtifactsAreRegisteredWithGroupHashAndLinkedState() throws IOException {
        createModule.execute(project, "alfa", List.of(Technician.ANALYST), claude);

        Path specialist = project.resolve(".claude/agents/analista-alfa.md").toAbsolutePath().normalize();
        var record = metadata.artifact(specialist).orElseThrow();
        assertEquals("project/specialist/analista", record.groupId());
        assertEquals(LinkState.LINKED, record.state());
        assertEquals(ContentHash.of(Files.readString(specialist)), record.hash());
        assertTrue(metadata.artifact(project.resolve(".claude/agents/analista-router.md").toAbsolutePath().normalize()).isPresent());
        assertEquals("project/skill/team-changes",
            metadata.artifact(project.resolve(".claude/skills/team-changes").toAbsolutePath().normalize()).orElseThrow().groupId());
        // El cuerpo compartido conserva los placeholders sin resolver
        assertTrue(metadata.group("project/specialist/analista").orElseThrow().sharedBody().contains("{module}"));
    }

    @Test
    void aNewModuleUsesTheSharedBodyOfTheGroupWhenItExists() throws IOException {
        createModule.execute(project, "alfa", List.of(Technician.ANALYST), claude);
        metadata.putGroup(new GroupRecord("project/specialist/analista", ArtifactScope.PROJECT,
            "Cuerpo personalizado de {module} en {slug-provider}.\n"));

        createModule.execute(project, "beta", List.of(Technician.ANALYST), claude);

        String beta = Files.readString(project.resolve(".claude/agents/analista-beta.md"));
        assertTrue(beta.endsWith("Cuerpo personalizado de beta en .claude.\n"));
        assertTrue(beta.contains("name: analista-beta"));
        // El módulo anterior no cambia
        assertFalse(Files.readString(project.resolve(".claude/agents/analista-alfa.md")).contains("personalizado"));
    }

    @Test
    void anInvalidNameOrProjectCreatesNothing() throws IOException {
        long before;
        try(var files = Files.walk(project)) {
            before = files.count();
        }

        assertEquals(AppErrorCode.MODULE_NAME_RESERVED, assertThrows(DomainException.class,
            () -> createModule.execute(project, "router", List.of(Technician.ANALYST), claude)).code());
        assertEquals(AppErrorCode.PROJECT_PATH_NOT_FOUND, assertThrows(DomainException.class,
            () -> createModule.execute(tmp.resolve("no-existe"), "alfa", List.of(Technician.ANALYST), claude)).code());

        try(var files = Files.walk(project)) {
            assertEquals(before, files.count());
        }
        assertFalse(Files.exists(project.resolve(".claude")));
        assertTrue(metadata.snapshot().artifacts().isEmpty());
    }

    @Test
    void aFailureInTheMiddleReturnsAPartialResultInsteadOfThrowing() throws IOException {
        // .claude/agents es un fichero: no se pueden generar agentes, pero el registro y las skills sí
        Files.createDirectories(project.resolve(".claude"));
        Files.writeString(project.resolve(".claude/agents"), "no soy una carpeta");

        OperationResult result = createModule.execute(project, "alfa", List.of(Technician.ANALYST), claude);

        assertTrue(result.created().stream().anyMatch(c -> c.contains("team.md")));
        assertTrue(result.created().stream().anyMatch(c -> c.contains("skills")));
        assertFalse(result.warnings().isEmpty());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("analista-alfa.md")));
        assertTrue(Files.readString(project.resolve(".sdd/team.md")).contains("- alfa | - | -"));
    }

    @Test
    void theTeamFileKeepsItsLineEndingsAndKeepsABackup() throws IOException {
        Path team = project.resolve(".sdd/team.md");
        Files.writeString(team, Files.readString(team).replace("\r\n", "\n").replace("\n", "\r\n"));

        createModule.execute(project, "alfa", List.of(Technician.ANALYST), claude);

        String content = Files.readString(team);
        assertTrue(content.contains("- alfa | - | -"));
        assertFalse(content.replace("\r\n", "").contains("\n"));
        try(var backups = Files.list(tmp.resolve("backups"))) {
            assertTrue(backups.anyMatch(b -> b.getFileName().toString().endsWith("team.md.bak")));
        }
    }
}
