package es.vargontoc.agentic.viewer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliLocatorTest {

    @TempDir
    Path temp;

    @Test
    void encuentraElJarDentroDelDirectorioDelViewer() throws IOException {
        Path viewer = temp.resolve("viewer");
        Path jar = viewer.resolve("cli/agentic-develop.jar");
        Files.createDirectories(jar.getParent());
        Files.writeString(jar, "dummy");

        assertEquals(jar.toAbsolutePath().normalize(), CliLocator.locate(viewer));
    }

    @Test
    void fallaSiNoEstaEnLaCarpetaCliDelViewer() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> CliLocator.locate(temp));

        assertTrue(error.getMessage().contains("agentic-develop.jar"));
        assertTrue(error.getMessage().contains("carpeta cli"));
    }
}
