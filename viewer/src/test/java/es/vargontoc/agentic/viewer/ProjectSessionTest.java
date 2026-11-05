package es.vargontoc.agentic.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.viewer.cli.CliClient;
import es.vargontoc.agentic.viewer.cli.CliClientException;
import es.vargontoc.agentic.viewer.cli.CliLocator;
import es.vargontoc.agentic.viewer.cli.CliProcess;
import es.vargontoc.agentic.viewer.cli.CliProcess.CliResult;
import es.vargontoc.agentic.viewer.cli.model.InitData;

class ProjectSessionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path temp;

    @Test
    void seleccionaUnProyectoConfigurado() {
        ProjectSession session = session(statusJson(true));

        session.select(temp);

        assertTrue(session.configured());
        assertEquals(temp.toAbsolutePath().normalize(), session.path());
        assertEquals(2, session.status().providers().size());
    }

    @Test
    void distingueUnProyectoNoConfigurado() {
        ProjectSession session = session(statusJson(false));

        session.select(temp);

        assertFalse(session.configured());
        assertFalse(session.status().project().initialized());
    }

    @Test
    void iniciaElProyectoYRefrescaElEstado() {
        AtomicReference<List<String>> lastArgs = new AtomicReference<>();
        ArrayDeque<CliResult> results = new ArrayDeque<>(List.of(
            result(statusJson(false)),
            result(initJson()),
            result(statusJson(true))));
        CliClient client = new CliClient((args, stdin, timeout) -> {
            lastArgs.set(List.copyOf(args));
            return results.removeFirst();
        }, mapper);
        ProjectSession session = new ProjectSession(client);
        session.select(temp);

        InitData init = session.init();

        assertTrue(init.created());
        assertTrue(session.configured());
        assertEquals(List.of("status", "--project", session.path().toString()), lastArgs.get());
    }

    @Test
    void informaDelErrorSinGuardarEstado() {
        CliClient client = new CliClient(
            (args, stdin, timeout) -> result(errorJson(), 1), mapper);
        ProjectSession session = new ProjectSession(client);

        assertThrows(CliClientException.class, () -> session.select(temp));
        assertNull(session.status());
        assertFalse(session.configured());
    }

    @Test
    void noPermiteIniciarSinProyecto() {
        ProjectSession session = session(statusJson(false));

        assertThrows(IllegalStateException.class, session::init);
    }

    @Test
    void iniciaUnProyectoRealSiElJarEstaDisponible() {
        CliClient client;
        try {
            client = new CliClient(
                new CliProcess(CliLocator.resolve(Path.of("").toAbsolutePath())), mapper);
        } catch (IllegalStateException exception) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "CLI no construido: " + exception.getMessage());
            return;
        }

        ProjectSession session = new ProjectSession(client);
        session.select(temp);
        assertFalse(session.configured());

        InitData init = session.init();

        assertTrue(init.created());
        assertTrue(session.configured());
        assertTrue(Files.exists(temp.resolve(".sdd")));
    }

    private ProjectSession session(String stdout) {
        CliClient client = new CliClient(
            (args, stdin, timeout) -> result(stdout), mapper);
        return new ProjectSession(client);
    }

    private static CliResult result(String stdout) {
        return result(stdout, 0);
    }

    private static CliResult result(String stdout, int exitCode) {
        return new CliResult(exitCode, stdout, "", false, 1);
    }

    private static String statusJson(boolean initialized) {
        return """
            {"schemaVersion":1,"command":"status","ok":true,"data":{"providers":[
              {"id":"claude","name":"Claude","state":"configured","missing":[]},
              {"id":"codex","name":"Codex","state":"partial","missing":[]}
            ],"project":{"path":"/proyecto","initialized":%s,"modules":["auth"],"warnings":[]}},"errors":[]}
            """.formatted(initialized);
    }

    private static String initJson() {
        return """
            {"schemaVersion":1,"command":"init","ok":true,
             "data":{"path":"/proyecto","created":true,"alreadyConfigured":false},"errors":[]}
            """;
    }

    private static String errorJson() {
        return """
            {"schemaVersion":1,"command":"status","ok":false,"data":null,
             "errors":[{"code":"not_found.project.path","path":"/no","message":"No existe."}]}
            """;
    }
}
