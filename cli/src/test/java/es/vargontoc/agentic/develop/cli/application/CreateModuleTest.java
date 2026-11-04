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
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.support.Console;

class CreateModuleTest {

    @TempDir
    Path tmp;

    Path project;
    final CreateModule createModule = new CreateModule();
    final List<Provider> claude = List.of(new Provider(ProviderType.CLAUDE, Path.of("ignorado")));

    @BeforeEach
    void setUp() throws IOException {
        project = Files.createDirectories(tmp.resolve("proyecto"));
        PathResolver paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        new ProjectWorkspace(paths, tmp.resolve("backups")).initialize(project);
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
}
