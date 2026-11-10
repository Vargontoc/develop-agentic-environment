package es.vargontoc.agentic.viewer.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WatcherPathsTest {

    @TempDir
    Path tempDir;

    @Test
    void incluyeSoloCarpetasDeArtefactos() throws Exception {
        Path project = Files.createDirectories(tempDir.resolve("proyecto"));
        Files.createDirectories(project.resolve(".claude/agents"));
        Files.createDirectories(project.resolve(".sdd"));
        Path home = Files.createDirectories(tempDir.resolve("home"));
        Files.createDirectories(home.resolve(".claude/agents"));
        Files.createDirectories(home.resolve(".codex/agents"));
        Files.createDirectories(home.resolve(".config/opencode/skills"));

        List<Path> directories = WatcherPaths.directories(project, home);

        assertTrue(directories.contains(project.resolve(".claude")));
        assertTrue(directories.contains(project.resolve(".sdd")));
        assertTrue(directories.contains(home.resolve(".claude/agents")));
        assertTrue(directories.contains(home.resolve(".codex/agents")));
        assertTrue(directories.contains(home.resolve(".config/opencode/skills")));
        assertFalse(directories.contains(project));
        assertFalse(directories.contains(home.resolve(".gemini/skills")));
    }

    @Test
    void ignoraDirectoriosPesados() {
        assertTrue(WatcherPaths.isIgnored(Path.of("/proyecto/target")));
        assertTrue(WatcherPaths.isIgnored(Path.of("/proyecto/node_modules")));
        assertTrue(WatcherPaths.isIgnored(Path.of("/proyecto/.git")));
        assertFalse(WatcherPaths.isIgnored(Path.of("/proyecto/.claude")));
        assertFalse(WatcherPaths.isIgnored(Path.of("/proyecto/specs")));
    }
}
