package es.vargontoc.agentic.viewer.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.agentic.viewer.cli.model.CliError;
import es.vargontoc.agentic.viewer.cli.model.DescribeData;
import es.vargontoc.agentic.viewer.cli.model.StatusData;
import es.vargontoc.agentic.viewer.cli.model.TreeData;

/**
 * Cliente tipado del contrato {@code cli-json}. Valida el envelope, la versión
 * de esquema y los errores, y convierte {@code data} al modelo correspondiente.
 */
public final class CliClient {

    private static final int SCHEMA_VERSION = 1;

    private final CliRunner runner;
    private final ObjectMapper mapper;

    public CliClient(CliRunner runner, ObjectMapper mapper) {
        this.runner = runner;
        this.mapper = mapper.copy()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public StatusData status(Path project) {
        List<String> args = new ArrayList<>(List.of("status"));
        addProject(args, project);
        return execute(args, StatusData.class);
    }

    public TreeData tree(Path project) {
        List<String> args = new ArrayList<>(List.of("tree"));
        addProject(args, project);
        return execute(args, TreeData.class);
    }

    public DescribeData describe(Path artifact, Path project) {
        Objects.requireNonNull(artifact, "artifact");
        List<String> args = new ArrayList<>(List.of("describe", "--path", artifact.toString()));
        addProject(args, project);
        return execute(args, DescribeData.class);
    }

    private void addProject(List<String> args, Path project) {
        if (project != null) {
            args.add("--project");
            args.add(project.toString());
        }
    }

    private <T> T execute(List<String> args, Class<T> type) {
        CliProcess.CliResult result = runner.run(args);

        if (result.timedOut()) {
            throw new CliClientException("client.timeout", null,
                "El CLI no respondió a tiempo.", -1);
        }

        String stdout = result.stdout() == null ? "" : result.stdout().replace("\uFEFF", "").trim();
        if (stdout.isEmpty()) {
            throw new CliClientException("client.empty_output", null,
                "El CLI no devolvió datos.", result.exitCode());
        }

        JsonNode root;
        try {
            root = mapper.readTree(stdout);
        } catch (JsonProcessingException exception) {
            throw new CliClientException("client.invalid_json", null,
                "La respuesta del CLI no es JSON válido: " + exception.getOriginalMessage(),
                result.exitCode());
        }

        int schemaVersion = root.path("schemaVersion").asInt(-1);
        if (schemaVersion != SCHEMA_VERSION) {
            throw new CliClientException("client.unsupported_schema", null,
                "Versión de contrato no compatible: " + schemaVersion, result.exitCode());
        }

        boolean ok = root.path("ok").asBoolean(false);
        List<CliError> errors = errors(root.get("errors"));
        if (!ok || !errors.isEmpty()) {
            CliError first = errors.isEmpty()
                ? new CliError("client.unknown_error", null, "El CLI devolvió ok=false sin errores.")
                : errors.getFirst();
            throw new CliClientException(first.code(), first.path(), first.message(), result.exitCode());
        }

        JsonNode data = root.get("data");
        if (data == null || data.isNull()) {
            throw new CliClientException("client.empty_data", null,
                "El comando no devolvió datos.", result.exitCode());
        }

        try {
            return mapper.treeToValue(data, type);
        } catch (JsonProcessingException exception) {
            throw new CliClientException("client.invalid_data", null,
                "Los datos del CLI no tienen la forma esperada: " + exception.getOriginalMessage(),
                result.exitCode());
        }
    }

    private List<CliError> errors(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        try {
            return mapper.convertValue(node, new TypeReference<List<CliError>>() { });
        } catch (IllegalArgumentException exception) {
            return List.of(new CliError("client.invalid_error", null,
                "No se pudo leer la lista de errores del CLI."));
        }
    }
}
