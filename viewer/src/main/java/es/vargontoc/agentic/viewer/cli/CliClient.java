package es.vargontoc.agentic.viewer.cli;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.agentic.viewer.cli.model.AdoptData;
import es.vargontoc.agentic.viewer.cli.model.CatalogData;
import es.vargontoc.agentic.viewer.cli.model.CliError;
import es.vargontoc.agentic.viewer.cli.model.ConfigureProviderData;
import es.vargontoc.agentic.viewer.cli.model.CreateArtifactData;
import es.vargontoc.agentic.viewer.cli.model.CreateModuleData;
import es.vargontoc.agentic.viewer.cli.model.DescribeData;
import es.vargontoc.agentic.viewer.cli.model.InitData;
import es.vargontoc.agentic.viewer.cli.model.SaveData;
import es.vargontoc.agentic.viewer.cli.model.StatusData;
import es.vargontoc.agentic.viewer.cli.model.TreeData;
import es.vargontoc.agentic.viewer.watcher.CacheStore;

/**
 * Cliente tipado del contrato {@code cli-json}. Valida el envelope, la versión
 * de esquema y los errores, y convierte {@code data} al modelo correspondiente.
 */
public final class CliClient {

    private static final int SCHEMA_VERSION = 1;

    private final CliRunner runner;
    private final ObjectMapper mapper;
    private final CacheStore cache;

    public CliClient(CliRunner runner, ObjectMapper mapper) {
        this(runner, mapper, null);
    }

    public CliClient(CliRunner runner, ObjectMapper mapper, CacheStore cache) {
        this.runner = runner;
        this.cache = cache;
        this.mapper = mapper.copy()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public StatusData status(Path project) {
        String key = CacheStore.statusKey(projectKey(project));
        return cached(key, () -> {
            List<String> args = new ArrayList<>(List.of("status"));
            addProject(args, project);
            return execute(args, StatusData.class);
        });
    }

    public InitData init(Path project) {
        Objects.requireNonNull(project, "project");
        return execute(List.of("init", "--project", project.toString()), InitData.class);
    }

    public TreeData tree(Path project) {
        String key = CacheStore.treeKey(projectKey(project));
        return cached(key, () -> {
            List<String> args = new ArrayList<>(List.of("tree"));
            addProject(args, project);
            return execute(args, TreeData.class);
        });
    }

    public DescribeData describe(Path artifact, Path project) {
        Objects.requireNonNull(artifact, "artifact");
        String key = CacheStore.describeKey(artifact.toString(), projectKey(project));
        return cached(key, () -> {
            List<String> args = new ArrayList<>(List.of("describe", "--path", artifact.toString()));
            addProject(args, project);
            return execute(args, DescribeData.class);
        });
    }

    /** Guarda el cuerpo de una instancia con {@code save --stdin}. */
    public SaveData saveBody(Path artifact, String content, Path project) {
        return saveBody(artifact, content, project, null);
    }

    /** Guarda el cuerpo de una instancia con {@code save --stdin}. */
    public SaveData saveBody(Path artifact, String content, Path project, String file) {
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(content, "content");
        List<String> args = new ArrayList<>(List.of("save", "--path", artifact.toString(), "--stdin"));
        if (file != null && !file.isBlank()) {
            args.add("--file");
            args.add(file);
        }
        addProject(args, project);
        return execute(args, content, SaveData.class);
    }

    /** Guarda la cabecera de un agente con {@code save --header-stdin} en modo instancia. */
    public SaveData saveHeader(Path artifact, String header, Path project) {
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(header, "header");
        List<String> args = new ArrayList<>(List.of("save", "--path", artifact.toString(),
            "--header-stdin", "--mode", "instance"));
        addProject(args, project);
        return execute(args, header, SaveData.class);
    }

    /** Propaga el cuerpo compartido a su grupo; con {@code dryRun} solo calcula los diffs. */
    public SaveData saveGroup(Path artifact, String sharedBody, boolean dryRun, Path project) {
        return saveGroup(artifact, sharedBody, dryRun, project, null);
    }

    /** Propaga el cuerpo compartido a su grupo; con {@code dryRun} solo calcula los diffs. */
    public SaveData saveGroup(Path artifact, String sharedBody, boolean dryRun, Path project, String file) {
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(sharedBody, "sharedBody");
        List<String> args = new ArrayList<>(List.of("save", "--path", artifact.toString(),
            "--mode", "group", "--stdin"));
        if (file != null && !file.isBlank()) {
            args.add("--file");
            args.add(file);
        }
        if (dryRun) {
            args.add("--dry-run");
        }
        addProject(args, project);
        return execute(args, sharedBody, SaveData.class);
    }

    /** Vincula un artefacto legacy o desvinculado a su grupo; con {@code confirm} escribe y registra. */
    public AdoptData adopt(Path artifact, Path project, boolean confirm) {
        Objects.requireNonNull(artifact, "artifact");
        List<String> args = new ArrayList<>(List.of("adopt", "--path", artifact.toString()));
        if (confirm) {
            args.add("--confirm");
        }
        addProject(args, project);
        return execute(args, AdoptData.class);
    }

    /** Crea un módulo con técnicos y proveedores; con {@code dryRun} solo simula. */
    public CreateModuleData createModule(Path project, String moduleName, List<String> technicians,
                                         List<String> providers, boolean dryRun) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(moduleName, "moduleName");
        List<String> args = new ArrayList<>(List.of(
            "create-module",
            "--project", project.toString(),
            "--module", moduleName,
            "--technicians", String.join(",", technicians),
            "--providers", String.join(",", providers)));
        if (dryRun) {
            args.add("--dry-run");
        }
        return execute(args, CreateModuleData.class);
    }

    /** Añade técnicos a un módulo existente; con {@code dryRun} solo simula. */
    public CreateModuleData addTechnician(Path project, String moduleName, List<String> technicians,
                                          List<String> providers, boolean dryRun) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(moduleName, "moduleName");
        List<String> args = new ArrayList<>(List.of(
            "add-technician",
            "--project", project.toString(),
            "--module", moduleName,
            "--technicians", String.join(",", technicians),
            "--providers", String.join(",", providers)));
        if (dryRun) {
            args.add("--dry-run");
        }
        return execute(args, CreateModuleData.class);
    }

    /** Aplica el catálogo global de un proveedor; lo existente se omite. */
    public ConfigureProviderData configureProvider(String provider) {
        Objects.requireNonNull(provider, "provider");
        return execute(List.of("configure-provider", "--provider", provider), ConfigureProviderData.class);
    }

    /**
     * Consulta el catálogo de artefactos disponibles para crear.
     * Si {@code name} no es null, devuelve solo esa entrada con su plantilla.
     */
    public CatalogData catalog(String provider, String scope, String kind, String name, Path project) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(kind, "kind");
        List<String> args = new ArrayList<>(List.of(
            "catalog",
            "--provider", provider,
            "--scope", scope,
            "--kind", kind));
        if (name != null && !name.isBlank()) {
            args.add("--name");
            args.add(name);
        }
        addProject(args, project);
        return execute(args, CatalogData.class);
    }

    /**
     * Crea un agente o skill desde el catálogo.
     * Si {@code content} no es null, se usa como contenido personalizado.
     * Si {@code overwrite} es true, sobrescribe artefactos existentes.
     */
    public CreateArtifactData createArtifact(String provider, String scope, String kind, String name,
                                             Path project, String content, boolean overwrite) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        List<String> args = new ArrayList<>(List.of(
            "create-artifact",
            "--provider", provider,
            "--scope", scope,
            "--kind", kind,
            "--name", name));
        if (content != null) {
            args.add("--stdin");
        }
        if (overwrite) {
            args.add("--overwrite");
        }
        addProject(args, project);
        return execute(args, content, CreateArtifactData.class);
    }

    private void addProject(List<String> args, Path project) {
        if (project != null) {
            args.add("--project");
            args.add(project.toString());
        }
    }

    private <T> T cached(String key, Supplier<T> loader) {
        return cache == null ? loader.get() : cache.getOrLoad(key, loader);
    }

    private static String projectKey(Path project) {
        return project == null ? null : project.toString();
    }

    private <T> T execute(List<String> args, Class<T> type) {
        return execute(args, null, type);
    }

    private <T> T execute(List<String> args, String stdin, Class<T> type) {
        CliProcess.CliResult result = runner.run(args, stdin, Duration.ofSeconds(15));

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
