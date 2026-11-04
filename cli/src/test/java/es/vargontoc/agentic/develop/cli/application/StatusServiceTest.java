package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import es.vargontoc.agentic.develop.cli.application.StatusService.StatusReport;
import es.vargontoc.agentic.develop.cli.domain.ProviderState;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.providers.LocalProviderCatalog;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;
import es.vargontoc.agentic.develop.cli.support.Console;

class StatusServiceTest {

    @TempDir
    Path tmp;

    Path project;
    ProjectWorkspace workspace;
    StatusService status;

    @BeforeEach
    void setUp() throws IOException {
        PathResolver paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        project = Files.createDirectories(tmp.resolve("proyecto"));
        workspace = new ProjectWorkspace(paths, tmp.resolve("backups"));
        status = new StatusService(new LocalProviderCatalog(paths, new ResourceArtifactCatalog()), workspace);
    }

    private ProviderState stateOf(StatusReport report, ProviderType type) {
        return report.providers().stream().filter(p -> p.type() == type).findFirst().orElseThrow().state();
    }

    @Test
    void reportsEveryProviderStateWithoutAProject() throws IOException {
        Files.createDirectories(tmp.resolve(".claude"));

        StatusReport report = status.status(null);

        assertEquals(4, report.providers().size());
        assertEquals(ProviderState.UNCONFIGURED, stateOf(report, ProviderType.CLAUDE));
        assertEquals(ProviderState.NOT_INSTALLED, stateOf(report, ProviderType.GEMINI));
        assertNull(report.project());
    }

    @Test
    void reportsTheProjectModulesAndWhetherItIsInitialized() throws IOException {
        workspace.initialize(project);
        Files.writeString(project.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\n- beta | - | -\n");

        StatusService.ProjectStatus info = status.status(project).project();

        assertTrue(info.initialized());
        assertEquals(List.of("alfa", "beta"), info.modules());
        assertTrue(info.warnings().isEmpty());
    }

    @Test
    void uninitializedProjectIsReportedWithAWarning() {
        StatusService.ProjectStatus info = status.status(project).project();

        assertEquals(false, info.initialized());
        assertEquals(List.of(new TeamWarning(TeamWarning.Type.TEAM_MISSING, 0)), info.warnings());
    }

    @Test
    void missingOrNonDirectoryProjectIsATypedErrorWithItsPath() throws IOException {
        Path file = Files.writeString(tmp.resolve("fichero"), "x");

        PathDomainException missing = assertThrows(PathDomainException.class, () -> status.status(tmp.resolve("nada")));
        PathDomainException notDir = assertThrows(PathDomainException.class, () -> status.status(file));

        assertEquals(AppErrorCode.PROJECT_PATH_NOT_FOUND, missing.code());
        assertEquals(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY, notDir.code());
    }

    @Test
    void readingStatusWritesNothingAndPrintsNothing() throws IOException {
        workspace.initialize(project);
        List<Path> before = listAll(tmp);

        String out = Console.capturedOut(() -> status.status(project));

        assertEquals("", out);
        assertEquals(before, listAll(tmp));
    }

    private static List<Path> listAll(Path root) throws IOException {
        try(var files = Files.walk(root)) {
            return files.sorted().toList();
        }
    }
}
