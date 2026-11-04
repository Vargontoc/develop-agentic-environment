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

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.team.TeamInfo;
import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

class ProjectWorkspaceTest {

    @TempDir
    Path tmp;

    Path project;
    ProjectWorkspace workspace;

    @BeforeEach
    void setUp() throws IOException {
        project = Files.createDirectories(tmp.resolve("proyecto"));
        PathResolver paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        workspace = new ProjectWorkspace(paths, tmp.resolve("backups"));
    }

    private Path team() {
        return project.resolve(".sdd").resolve("team.md");
    }

    @Test
    void projectWithoutSddIsInitialized() {
        ProjectWorkspace.InitResult result = workspace.initialize(project);

        assertFalse(result.alreadyConfigured());
        assertTrue(Files.isRegularFile(team()));
        assertTrue(Files.isRegularFile(project.resolve(".sdd").resolve("architecture.md")));
    }

    @Test
    void projectWithSddIsNeverOverwritten() throws IOException {
        workspace.initialize(project);
        Files.writeString(team(), "mis cambios");
        Files.delete(project.resolve(".sdd").resolve("architecture.md"));

        ProjectWorkspace.InitResult second = workspace.initialize(project);

        assertTrue(second.alreadyConfigured());
        assertEquals("mis cambios", Files.readString(team()));
        assertFalse(Files.exists(project.resolve(".sdd").resolve("architecture.md")));
    }

    @Test
    void missingOrNonDirectoryPathsAreRejected() throws IOException {
        Path file = Files.writeString(tmp.resolve("fichero"), "x");

        DomainException missing = assertThrows(DomainException.class, () -> workspace.initialize(tmp.resolve("nada")));
        DomainException notDir = assertThrows(DomainException.class, () -> workspace.initialize(file));

        assertEquals(AppErrorCode.PROJECT_PATH_NOT_FOUND, missing.code());
        assertEquals(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY, notDir.code());
    }

    @Test
    void freshProjectTeamHasNoModulesAndNoWarnings() {
        workspace.initialize(project);

        TeamInfo info = workspace.readTeam(project);

        assertTrue(info.modules().isEmpty());
        assertTrue(info.warnings().isEmpty());
    }

    @Test
    void registeredModulesAreReadFromTeamFile() throws IOException {
        workspace.initialize(project);
        Files.writeString(team(), "## Módulos\n\n- alfa | cli | cli/src\n- beta | - | -\n");

        assertEquals(List.of("alfa", "beta"), workspace.readTeam(project).moduleNames());
    }

    @Test
    void missingTeamFileOrNonDirectoryProjectGivesAWarningAndNoModules() throws IOException {
        Path file = Files.writeString(tmp.resolve("fichero"), "x");

        for(Path path : List.of(project, file, tmp.resolve("no-existe"))) {
            TeamInfo info = workspace.readTeam(path);

            assertTrue(info.modules().isEmpty());
            assertEquals(List.of(new TeamWarning(TeamWarning.Type.TEAM_MISSING, 0)), info.warnings());
        }
    }

    @Test
    void unreadableTeamFileGivesAWarning() throws IOException {
        workspace.initialize(project);
        Files.write(team(), new byte[] {(byte) 0xC3, (byte) 0x28});

        TeamInfo info = workspace.readTeam(project);

        assertEquals(List.of(new TeamWarning(TeamWarning.Type.TEAM_UNREADABLE, 0)), info.warnings());
    }

    @Test
    void malformedTeamFileStillReturnsTheReadableModules() throws IOException {
        workspace.initialize(project);
        Files.writeString(team(), "## Módulos\n\n- alfa | - | -\nbasura\n- alfa | - | -\n");

        TeamInfo info = workspace.readTeam(project);

        assertEquals(List.of("alfa"), info.moduleNames());
        assertEquals(2, info.warnings().size());
    }
}
