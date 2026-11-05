package es.vargontoc.agentic.viewer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.viewer.cli.CliProcess.CliResult;
import es.vargontoc.agentic.viewer.cli.model.DescribeData;
import es.vargontoc.agentic.viewer.cli.model.StatusData;
import es.vargontoc.agentic.viewer.cli.model.TreeData;

class CliClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parseaStatus() {
        StatusData data = client(statusJson()).status(null);

        assertEquals(2, data.providers().size());
        assertEquals("claude", data.providers().getFirst().id());
        assertEquals("configured", data.providers().getFirst().state());
        assertNull(data.project());
    }

    @Test
    void parseaTreeConHijos() {
        TreeData data = client(treeJson()).tree(null);

        assertEquals(1, data.global().size());
        TreeData.Node provider = data.global().getFirst();
        assertEquals("provider", provider.type());
        assertEquals(1, provider.children().size());
        assertEquals("sdd-agent", provider.children().getFirst().name());
        assertEquals("linked", provider.children().getFirst().status());
        assertTrue(data.project().isEmpty());
    }

    @Test
    void parseaDescribeConCuerpoCompartido() {
        DescribeData data = client(describeJson()).describe(Path.of("agente.md"), null);

        assertEquals("claude", data.provider());
        assertEquals("global", data.scope());
        assertEquals("agent", data.kind());
        assertEquals("markdown", data.format());
        assertEquals("---", data.header());
        assertEquals("cuerpo", data.body());
        assertEquals("cuerpo compartido", data.sharedBody());
        assertFalse(data.modifiedExternally());
        assertTrue(data.files().isEmpty());
    }

    @Test
    void convierteUnErrorDelCliEnExcepcion() {
        CliClientException error = assertThrows(CliClientException.class,
            () -> client(errorJson(), 1).status(null));

        assertEquals("not_found.project.path", error.code());
        assertEquals("C:/no-existe", error.path());
        assertEquals("No existe el proyecto.", error.getMessage());
        assertEquals(1, error.exitCode());
    }

    @Test
    void rechazaUnaVersionDeEsquemaDesconocida() {
        CliClientException error = assertThrows(CliClientException.class,
            () -> client("""
                {"schemaVersion":2,"command":"status","ok":true,"data":{},"errors":[]}
                """).status(null));

        assertEquals("client.unsupported_schema", error.code());
    }

    @Test
    void rechazaJsonMalFormado() {
        CliClientException error = assertThrows(CliClientException.class,
            () -> client("esto no es json").status(null));

        assertEquals("client.invalid_json", error.code());
    }

    @Test
    void rechazaSalidaVacia() {
        CliClientException error = assertThrows(CliClientException.class,
            () -> client("").status(null));

        assertEquals("client.empty_output", error.code());
    }

    @Test
    void informaDeTimeout() {
        CliClient client = new CliClient(
            (args, stdin, timeout) -> new CliResult(-1, "", "", true, 1), mapper);

        CliClientException error = assertThrows(CliClientException.class,
            () -> client.status(null));

        assertEquals("client.timeout", error.code());
    }

    @Test
    void enviaElProyectoComoArgumento() {
        AtomicReference<List<String>> seen = new AtomicReference<>();
        CliClient client = new CliClient((args, stdin, timeout) -> {
            seen.set(List.copyOf(args));
            return new CliResult(0, statusJson(), "", false, 1);
        }, mapper);
        Path project = Path.of("proyectos", "con espacios");

        client.status(project);

        assertEquals(List.of("status", "--project", project.toString()), seen.get());
    }

    @Test
    void enviaLaRutaYElProyectoEnDescribe() {
        AtomicReference<List<String>> seen = new AtomicReference<>();
        CliClient client = new CliClient((args, stdin, timeout) -> {
            seen.set(List.copyOf(args));
            return new CliResult(0, describeJson(), "", false, 1);
        }, mapper);
        Path artifact = Path.of("tmp", "agente.md");
        Path project = Path.of("tmp", "proyecto");

        client.describe(artifact, project);

        assertEquals(List.of("describe", "--path", artifact.toString(), "--project", project.toString()), seen.get());
    }

    @Test
    void parseaStatusDelCliRealSiElJarEstaDisponible() {
        StatusData data = realClient().status(null);

        assertFalse(data.providers().isEmpty());
    }

    @Test
    void parseaTreeDelCliRealSiElJarEstaDisponible() {
        TreeData data = realClient().tree(null);

        assertFalse(data.global().isEmpty());
    }

    @Test
    void parseaDescribeDelCliRealSiHayUnAgente() {
        CliClient client = realClient();
        TreeData tree = client.tree(null);
        Path agent = tree.global().stream()
            .flatMap(provider -> provider.children() == null ? java.util.stream.Stream.empty() : provider.children().stream())
            .filter(node -> "agent".equals(node.type()) && node.path() != null)
            .map(node -> Path.of(node.path()))
            .findFirst()
            .orElse(null);
        org.junit.jupiter.api.Assumptions.assumeTrue(agent != null, "No hay agentes instalados");

        DescribeData data = client.describe(agent, null);

        assertEquals("agent", data.kind());
        assertTrue(data.body() != null && !data.body().isEmpty());
    }

    private CliClient realClient() {
        try {
            CliCommand command = CliLocator.resolve(Path.of("").toAbsolutePath());
            return new CliClient(new CliProcess(command), mapper);
        } catch (IllegalStateException exception) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "CLI no construido: " + exception.getMessage());
            return null;
        }
    }

    private CliClient client(String stdout) {
        return client(stdout, 0);
    }

    private CliClient client(String stdout, int exitCode) {
        return new CliClient(
            (args, stdin, timeout) -> new CliResult(exitCode, stdout, "", false, 1), mapper);
    }

    private static String statusJson() {
        return """
            {"schemaVersion":1,"command":"status","ok":true,"data":{"providers":[
              {"id":"claude","name":"Claude","state":"configured","missing":[]},
              {"id":"codex","name":"Codex","state":"partial","missing":["agents/sdd-agent.md"]}
            ],"project":null},"errors":[]}
            """;
    }

    private static String treeJson() {
        return """
            {"schemaVersion":1,"command":"tree","ok":true,"data":{
              "global":[{"type":"provider","name":"Claude","path":"/home/.claude","provider":"claude",
                "status":null,"pending":false,"children":[
                  {"type":"agent","name":"sdd-agent","path":"/home/.claude/agents/sdd-agent.md",
                   "provider":"claude","status":"linked","pending":false,"children":[]}
                ]}],
              "project":[],"warnings":[],"problems":[]},"errors":[]}
            """;
    }

    private static String describeJson() {
        return """
            {"schemaVersion":1,"command":"describe","ok":true,"data":{
              "provider":"claude","scope":"global","kind":"agent","name":"sdd-agent",
              "path":"/home/.claude/agents/sdd-agent.md","format":"markdown","module":null,
              "header":"---","body":"cuerpo","sharedBody":"cuerpo compartido",
              "hash":"abc123","status":"linked","modifiedExternally":false,"files":[]},"errors":[]}
            """;
    }

    private static String errorJson() {
        return """
            {"schemaVersion":1,"command":"status","ok":false,"data":null,
             "errors":[{"code":"not_found.project.path","path":"C:/no-existe",
                        "message":"No existe el proyecto."}]}
            """;
    }
}
