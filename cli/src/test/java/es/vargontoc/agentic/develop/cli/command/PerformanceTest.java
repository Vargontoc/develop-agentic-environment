package es.vargontoc.agentic.develop.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * NFR-002: cada comando en menos de 3 s con un proyecto de hasta 200 artefactos y {@code describe} en menos de 1 s
 * con un fichero de 1 MB. Se mide dentro del proceso y en frío (sin calentamiento), contando la serialización del
 * envelope; el arranque de la JVM queda fuera y se informa aparte en docs/performance. Los tiempos medidos se escriben
 * en la salida estándar de la prueba.
 */
@Tag("performance")
class PerformanceTest {

    private static final JsonMapper JSON = JsonMapperFactory.create();
    private static final Duration LIST_LIMIT = Duration.ofSeconds(3);
    private static final Duration DESCRIBE_LIMIT = Duration.ofSeconds(1);
    private static final int MODULES = 64;

    @TempDir
    static Path tmp;

    static Path home;
    static Path project;
    static CommandLine cli;
    static Services services;

    @BeforeAll
    static void build() throws IOException {
        home = Files.createDirectories(tmp.resolve("home"));
        project = Files.createDirectories(tmp.resolve("proyecto"));
        services = Services.of(new PathResolver(Map.of("HOME", home.toString()), PathResolver.Os.LINUX));
        cli = new CommandLine(Commands.standard(() -> services));

        run("configure-provider", "--provider", "claude");
        run("init", "--project", project.toString());
        List<Provider> claude = List.of(new Provider(ProviderType.CLAUDE, project));
        for(int i = 0; i < MODULES; i++)
            services.createModule().execute(project, "modulo-" + letters(i), List.of(Technician.values()), claude);
    }

    // Los nombres de módulo solo admiten letras: 0 -> aa, 1 -> ab...
    private static String letters(int n) {
        return "" + (char) ('a' + n / 26) + (char) ('a' + n % 26);
    }

    private static String raw(String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        assertEquals(0, cli.run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8)), String.join(" ", args));
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static Map<?, ?> run(String... args) {
        return JSON.readValue(raw(args), Map.class);
    }

    private static Duration time(String label, Runnable action) {
        long start = System.nanoTime();
        action.run();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        System.out.println("[perf] " + label + ": " + elapsed.toMillis() + " ms");
        return elapsed;
    }

    private static long artifactsInProject() throws IOException {
        try(Stream<Path> files = Files.list(project.resolve(".claude/agents"))) {
            return files.count() + Files.list(project.resolve(".claude/skills")).count();
        }
    }

    @Test
    void theProjectHasAtLeast200Artifacts() throws IOException {
        assertTrue(artifactsInProject() >= 200, "artefactos: " + artifactsInProject());
    }

    @Test
    void statusWithAProjectOf200ArtifactsTakesLessThan3Seconds() {
        Duration elapsed = time("status (proyecto de 200+ artefactos)", () -> run("status", "--project", project.toString()));

        assertTrue(elapsed.compareTo(LIST_LIMIT) < 0, elapsed.toString());
    }

    @Test
    void treeWithAProjectOf200ArtifactsTakesLessThan3Seconds() {
        Duration elapsed = time("tree (proyecto de 200+ artefactos)", () -> run("tree", "--project", project.toString()));

        assertTrue(elapsed.compareTo(LIST_LIMIT) < 0, elapsed.toString());
    }

    @Test
    void describeOfA1MegabyteArtifactTakesLessThan1Second() throws IOException {
        Path agent = project.resolve(".claude/agents/analista-modulo-aa.md");
        String body = "linea de texto de relleno para alcanzar un megabyte de contenido\n";
        StringBuilder big = new StringBuilder(Files.readString(agent));
        while(big.length() < 1_048_576)
            big.append(body);
        Files.writeString(agent, big.toString());

        // Sin interpretar la salida: el lector JSON por defecto rechaza cadenas de más de 1.000.000 de caracteres
        String[] output = new String[1];
        Duration elapsed = time("describe (1 MB)",
            () -> output[0] = raw("describe", "--path", agent.toString(), "--project", project.toString()));

        assertTrue(Files.size(agent) >= 1_048_576);
        assertTrue(output[0].startsWith("{\"schemaVersion\"") && output[0].contains("\"ok\":true"), "envelope");
        assertTrue(elapsed.compareTo(DESCRIBE_LIMIT) < 0, elapsed.toString());
    }

    @Test
    void manyLargeArtifactsStillListWithinTheLimit() throws IOException {
        List<Path> agents = new ArrayList<>();
        try(Stream<Path> files = Files.list(project.resolve(".claude/agents"))) {
            files.filter(f -> f.getFileName().toString().startsWith("desarrollador-modulo-")).forEach(agents::add);
        }
        String filler = "x".repeat(99) + "\n";
        for(Path agent : agents) {
            StringBuilder big = new StringBuilder(Files.readString(agent));
            for(int i = 0; i < 1000; i++)
                big.append(filler);
            Files.writeString(agent, big.toString());
        }

        Duration elapsed = time("tree (" + agents.size() + " artefactos de ~100 KB)",
            () -> run("tree", "--project", project.toString()));

        assertTrue(elapsed.compareTo(LIST_LIMIT) < 0, elapsed.toString());
    }
}
