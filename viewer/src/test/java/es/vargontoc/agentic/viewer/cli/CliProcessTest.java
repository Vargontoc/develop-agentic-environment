package es.vargontoc.agentic.viewer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliProcessTest {

    @TempDir
    Path temp;

    @Test
    void devuelveSalidaYCodigoCero() {
        CliProcess.CliResult result = process().run(List.of("ok"), null, Duration.ofSeconds(5));

        assertEquals(0, result.exitCode());
        assertFalse(result.timedOut());
        assertTrue(result.stdout().contains("\"ok\":true"));
        assertEquals("", result.stderr());
    }

    @Test
    void conservaErrorYCodigoDistintoDeCero() {
        CliProcess.CliResult result = process().run(List.of("fail"), null, Duration.ofSeconds(5));

        assertEquals(1, result.exitCode());
        assertEquals("fallo", result.stderr());
    }

    @Test
    void enviaLaEntradaEstandar() {
        CliProcess.CliResult result = process().run(List.of("echo"), "hola CLI", Duration.ofSeconds(5));

        assertEquals(0, result.exitCode());
        assertEquals("hola CLI", result.stdout());
    }

    @Test
    void cortaElProcesoSiSuperaElTiempo() {
        CliProcess.CliResult result = process().run(List.of("sleep"), null, Duration.ofMillis(200));

        assertTrue(result.timedOut());
        assertTrue(result.durationMillis() < 2500);
    }

    @Test
    void soportaRutasConEspacios() throws IOException {
        Path spaced = temp.resolve("con espacios");
        Path packageDir = spaced.resolve("es/vargontoc/agentic/viewer/cli");
        Files.createDirectories(packageDir);
        try (InputStream classFile = FakeCli.class.getResourceAsStream("FakeCli.class")) {
            Files.copy(classFile, packageDir.resolve("FakeCli.class"));
        }

        CliCommand command = new CliCommand(javaExecutable(),
            List.of("-cp", spaced.toString(), FakeCli.class.getName()), null);
        CliProcess.CliResult result = new CliProcess(command).run(List.of("ok"), null, Duration.ofSeconds(5));

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("\"ok\":true"));
    }

    @Test
    void ejecutaElCliRealSiElJarEstaDisponible() {
        CliCommand command;
        try {
            command = CliLocator.resolve(Path.of("").toAbsolutePath());
        } catch (IllegalStateException exception) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "CLI no construido: " + exception.getMessage());
            return;
        }

        CliProcess.CliResult result = new CliProcess(command).run(List.of("--version"), null, Duration.ofSeconds(15));

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("\"command\":\"version\""));
    }

    @Test
    void ejecutaStatusDelCliRealSiElJarEstaDisponible() {
        CliCommand command;
        try {
            command = CliLocator.resolve(Path.of("").toAbsolutePath());
        } catch (IllegalStateException exception) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "CLI no construido: " + exception.getMessage());
            return;
        }

        CliProcess.CliResult result = new CliProcess(command).run(List.of("status"), null, Duration.ofSeconds(15));

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("\"command\":\"status\""));
    }

    private CliProcess process() {
        String classpath = System.getProperty("java.class.path");
        CliCommand command = new CliCommand(javaExecutable(),
            List.of("-cp", classpath, FakeCli.class.getName()), null);
        return new CliProcess(command);
    }

    private static Path javaExecutable() {
        String name = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", name);
    }
}
