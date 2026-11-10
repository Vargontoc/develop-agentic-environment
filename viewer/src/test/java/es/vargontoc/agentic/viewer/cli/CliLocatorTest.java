package es.vargontoc.agentic.viewer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

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

    @Test
    void prefiereElCliRecienCompiladoEnDesarrollo() throws IOException {
        Path viewer = temp.resolve("viewer");
        Path bundled = viewer.resolve("cli/agentic-develop.jar");
        Files.createDirectories(bundled.getParent());
        Files.writeString(bundled, "old");
        Files.setLastModifiedTime(bundled, FileTime.fromMillis(1_000));

        Path development = temp.resolve("cli/target/agentic-develop.jar");
        Files.createDirectories(development.getParent());
        Files.writeString(development, "new");
        Files.setLastModifiedTime(development, FileTime.fromMillis(2_000));

        assertEquals(development.toAbsolutePath().normalize(), CliLocator.locate(viewer));
    }

    @Test
    void usaElJarEmpaquetadoSiEsMasNuevoQueElDeDesarrollo() throws IOException {
        Path viewer = temp.resolve("viewer");
        Path bundled = viewer.resolve("cli/agentic-develop.jar");
        Files.createDirectories(bundled.getParent());
        Files.writeString(bundled, "new");
        Files.setLastModifiedTime(bundled, FileTime.fromMillis(2_000));

        Path development = temp.resolve("cli/target/agentic-develop.jar");
        Files.createDirectories(development.getParent());
        Files.writeString(development, "old");
        Files.setLastModifiedTime(development, FileTime.fromMillis(1_000));

        assertEquals(bundled.toAbsolutePath().normalize(), CliLocator.locate(viewer));
    }
}
