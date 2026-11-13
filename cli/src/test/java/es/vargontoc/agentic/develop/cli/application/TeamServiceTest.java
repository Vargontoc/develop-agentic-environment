package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.team.ModuleEntry;
import es.vargontoc.agentic.develop.cli.domain.team.TeamOptions;
import es.vargontoc.agentic.develop.cli.domain.team.TeamParser;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

class TeamServiceTest {

    private static final String TEAM = """
        # Equipo

        ## Módulos

        - alfa | - | -

        ## Opciones

        - sprints: si
        - git-policy: balanced
        - contract-strategy: implementation-first

        ## Notas

        - se conserva
        """;

    @TempDir
    Path tmp;

    Path project;
    Path team;
    TeamService service;

    @BeforeEach
    void setUp() throws IOException {
        project = Files.createDirectories(tmp.resolve("proyecto"));
        Path sdd = Files.createDirectories(project.resolve(".sdd"));
        team = sdd.resolve("team.md");
        Files.writeString(team, TEAM);
        service = new TeamService(new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX),
            tmp.resolve("backups"));
    }

    @Test
    void readsModulesOptionsAndHash() {
        TeamService.TeamView view = service.read(project);

        assertEquals(team, view.path());
        assertFalse(view.hash().isBlank());
        assertEquals(List.of("alfa"), view.modules().stream().map(TeamService.ModuleSpec::name).toList());
        assertEquals(TeamOptions.DEFAULTS, view.options());
        assertTrue(view.warnings().isEmpty());
    }

    @Test
    void updatesModulesAndOptionsAndPreservesOtherSections() throws IOException {
        TeamService.UpdateResult result = service.update(project,
            new TeamOptions("no", "strict", "contract-first", "alfa"),
            List.of(new TeamService.ModuleSpec("alfa", "ALFA", List.of("src/alfa")),
                new TeamService.ModuleSpec("beta", null, List.of())),
            null, false);

        assertTrue(result.written());
        assertNotNull(result.backup());
        assertTrue(Files.isRegularFile(result.backup()));
        String content = Files.readString(team);
        assertTrue(content.contains("- alfa | ALFA | src/alfa"));
        assertTrue(content.contains("- beta | - | -"));
        assertTrue(content.contains("- sprints: no"));
        assertTrue(content.contains("- git-policy: strict"));
        assertTrue(content.contains("- contract-strategy: contract-first"));
        assertTrue(content.contains("- readme-owner: alfa"));
        assertTrue(content.contains("## Notas"));
        assertTrue(content.contains("- se conserva"));

        List<ModuleEntry> modules = TeamParser.parseDocument(content).modules();
        assertEquals(List.of("alfa", "beta"), modules.stream().map(ModuleEntry::name).toList());
        assertEquals("ALFA", modules.getFirst().mapId());
        assertEquals(List.of("src/alfa"), modules.getFirst().paths());
    }

    @Test
    void dryRunReturnsDiffWithoutWriting() throws IOException {
        String before = Files.readString(team);

        TeamService.UpdateResult result = service.update(project,
            new TeamOptions("no", "balanced", "implementation-first", null),
            List.of(new TeamService.ModuleSpec("alfa", "-", List.of())), null, true);

        assertTrue(result.dryRun());
        assertTrue(result.written());
        assertFalse(result.diff().isBlank());
        assertEquals(before, Files.readString(team));
    }

    @Test
    void staleHashIsAConflict() {
        DomainException error = assertThrows(DomainException.class, () -> service.update(project,
            TeamOptions.DEFAULTS, List.of(new TeamService.ModuleSpec("alfa", null, List.of())),
            "hash-viejo", true));

        assertEquals(AppErrorCode.TEAM_CONFLICT, error.code());
    }

    @Test
    void duplicateModulesAreRejected() {
        DomainException error = assertThrows(DomainException.class, () -> service.update(project,
            TeamOptions.DEFAULTS,
            List.of(new TeamService.ModuleSpec("alfa", null, List.of()),
                new TeamService.ModuleSpec("ALFA", null, List.of())),
            null, true));

        assertEquals(AppErrorCode.TEAM_MODULE_DUPLICATE, error.code());
    }

    @Test
    void readmeOwnerMustExistAndOptionsMustBeValid() {
        DomainException owner = assertThrows(DomainException.class, () -> service.update(project,
            new TeamOptions("si", "balanced", "implementation-first", "beta"),
            List.of(new TeamService.ModuleSpec("alfa", null, List.of())), null, true));
        DomainException option = assertThrows(DomainException.class, () -> service.update(project,
            new TeamOptions("quizas", "balanced", "implementation-first", null),
            List.of(new TeamService.ModuleSpec("alfa", null, List.of())), null, true));

        assertEquals(AppErrorCode.TEAM_OPTION_INVALID, owner.code());
        assertEquals(AppErrorCode.TEAM_OPTION_INVALID, option.code());
    }

    @Test
    void missingTeamFileIsReported() throws IOException {
        Files.delete(team);

        DomainException error = assertThrows(DomainException.class, () -> service.read(project));

        assertEquals(AppErrorCode.TEAM_FILE_NOT_FOUND, error.code());
    }

    @Test
    void commentsBetweenOptionItemsArePreserved() throws IOException {
        String content = """
            ## Módulos

            - alfa | - | -

            ## Opciones

            - sprints: si
            <!-- comentario entre opciones -->
            - git-policy: balanced
            - contract-strategy: implementation-first
            """;
        Files.writeString(team, content);

        service.update(project, TeamOptions.DEFAULTS,
            List.of(new TeamService.ModuleSpec("alfa", null, List.of())), null, false);

        String result = Files.readString(team);
        assertTrue(result.contains("<!-- comentario entre opciones -->"));
        assertTrue(result.contains("- sprints: si"));
        assertTrue(result.contains("- contract-strategy: implementation-first"));
    }

    @Test
    void crlfIsPreservedOnWrite() throws IOException {
        Files.writeString(team, TEAM.replace("\n", "\r\n"));

        service.update(project, TeamOptions.DEFAULTS,
            List.of(new TeamService.ModuleSpec("alfa", null, List.of("src/alfa"))), null, false);

        assertTrue(Files.readString(team).contains("\r\n"));
    }
}
