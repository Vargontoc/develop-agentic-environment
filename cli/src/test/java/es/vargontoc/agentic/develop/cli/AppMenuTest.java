package es.vargontoc.agentic.develop.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.support.Console;

/** El menú interactivo de extremo a extremo sobre un home temporal: cada opción llega a su servicio. */
class AppMenuTest {

    @TempDir
    Path tmp;

    Path home;
    Path project;
    Services services;

    @BeforeEach
    void setUp() throws IOException {
        home = Files.createDirectories(tmp.resolve("home"));
        Files.createDirectories(home.resolve(".claude"));
        project = Files.createDirectories(tmp.resolve("proyecto"));
        services = Services.of(new PathResolver(Map.of("HOME", home.toString()), PathResolver.Os.LINUX));
    }

    private String menu(String... lines) {
        String input = String.join("\n", lines) + "\n";
        return Console.capturedOut(() -> App.runMenu(services, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void theUsualMenuIsShownAndExitEndsNormally() {
        String out = menu("4");

        assertTrue(out.contains("1. Configuración entorno"), out);
        assertTrue(out.contains("2. Iniciar proyecto"), out);
        assertTrue(out.contains("3. Crear módulo"), out);
        assertTrue(out.contains("4. Salir"), out);
    }

    @Test
    void anInvalidOptionShowsTheErrorAndComesBackToTheMenu() {
        String out = menu("9", "4");

        assertTrue(out.contains("[ERROR] Opción no válida"), out);
        assertEquals(2, out.split("4\\. Salir", -1).length - 1);
    }

    @Test
    void endOfInputEndsTheMenuToo() {
        assertTrue(menu().contains("4. Salir"));
    }

    @Test
    void configureInstallsInTheChosenProviderAndRegistersWhatItInstalled() {
        String out = menu("1", "s", "1", "", "4");

        assertTrue(out.contains("Configurando Claude"), out);
        assertTrue(out.contains("Copied agent 'sdd-agent.md'"), out);
        assertTrue(Files.isRegularFile(home.resolve(".claude/agents/sdd-agent.md")));
        assertTrue(Files.isRegularFile(home.resolve(".claude/skills/sdd-design/SKILL.md")));
        assertTrue(Files.isRegularFile(home.resolve(".local/state/agentic-develop/metadata.json")));
    }

    @Test
    void startProjectCreatesTheSddStructure() {
        String out = menu("2", project.toString(), "4");

        assertTrue(out.contains("Proyecto iniciado en"), out);
        assertTrue(Files.isDirectory(project.resolve(".sdd")));
    }

    @Test
    void createModuleOffersToStartTheProjectAndThenCreatesTheModule() throws IOException {
        String out = menu("3", "s", project.toString(), "alfa", "1", "", "1", "", "4");

        assertTrue(out.contains("Módulo 'alfa' procesado"), out);
        assertTrue(Files.readString(project.resolve(".sdd/team.md")).contains("- alfa | - | -"));
        assertTrue(Files.isRegularFile(project.resolve(".claude/agents/analista-alfa.md")));
    }

    @Test
    void anInvalidModuleNameShowsTheErrorAndAsksAgain() {
        String out = menu("3", "s", project.toString(), "a1", "0", "4");

        assertTrue(out.contains("[ERROR] El nombre solo admite letras (a-z) y guiones"), out);
        assertFalse(Files.exists(project.resolve(".claude")));
    }

    @Test
    void withoutAnyCompatibleProviderTheMenuDoesNotStart() throws IOException {
        Path empty = Files.createDirectories(tmp.resolve("vacio"));
        Services bare = Services.of(new PathResolver(Map.of("HOME", empty.toString()), PathResolver.Os.LINUX));

        DomainException e = assertThrows(DomainException.class,
            () -> App.runMenu(bare, new ByteArrayInputStream(new byte[0])));

        assertEquals(AppErrorCode.INCOMPATIBLE_ENVIRONMENT, e.code());
    }
}
