package es.vargontoc.agentic.develop.cli.infrastructure.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Paridad entre las dos variantes del hook {@code sdd-specs-only}: los mismos casos se ejecutan contra el {@code .bat}
 * (solo en Windows) y contra el {@code .sh} (donde haya {@code bash} y {@code jq}) y los dos deben dar el resultado
 * esperado: 0 permite la escritura y 2 la bloquea. Se ejecutan los scripts reales del recurso, no una copia.
 * <p>
 * Diferencia conocida que NO se prueba: el {@code .bat} compara rutas sin distinguir mayúsculas y el {@code .sh}
 * distinguiéndolas, como cada sistema de ficheros.
 */
class HookParityTest {

    private static final int ALLOW = 0;
    private static final int DENY = 2;
    private static final JsonMapper JSON = JsonMapperFactory.create();
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    /**
     * El bash que ejecuta el {@code .sh}. En Windows, {@code bash} a secas resuelve al lanzador de WSL ({@code System32}),
     * que no ve las rutas de Windows: se busca Git Bash (o el que indique la variable {@code SDD_TEST_BASH}).
     */
    private static final String BASH = findBash();

    private static String findBash() {
        if(!WINDOWS)
            return "bash";
        List<String> candidates = new ArrayList<>();
        candidates.add(System.getenv("SDD_TEST_BASH"));
        for(String base : new String[] {"ProgramFiles", "ProgramFiles(x86)"})
            candidates.add(System.getenv(base) == null ? null : System.getenv(base) + "\\Git\\bin\\bash.exe");
        candidates.add(System.getenv("LOCALAPPDATA") == null ? null
            : System.getenv("LOCALAPPDATA") + "\\Programs\\Git\\bin\\bash.exe");
        return candidates.stream().filter(c -> c != null && Files.isRegularFile(Path.of(c))).findFirst().orElse(null);
    }

    @TempDir
    Path tmp;

    Path root;
    Path outside;

    /**
     * @param file  {@code file_path} del JSON (vacío = sin la propiedad); {@code {ROOT}} y {@code {OUTSIDE}} se sustituyen
     *              por rutas absolutas en el formato de cada sistema
     * @param agent {@code agent_type} (vacío = sin la propiedad)
     * @param cwd   {@code cwd} relativo a la raíz del proyecto; {@code null} = sin la propiedad (se usa el directorio
     *              del proceso, que es la raíz)
     * @param raw   si no es {@code null}, es la entrada estándar tal cual (JSON inválido, por ejemplo)
     */
    private record Case(String name, String policy, String file, String agent, String cwd, String raw, int expected) {
        static Case of(String name, String policy, String file, int expected) {
            return new Case(name, policy, file, "", "", null, expected);
        }

        Case agent(String value) {
            return new Case(name, policy, file, value, cwd, raw, expected);
        }

        Case cwd(String value) {
            return new Case(name, policy, file, agent, value, raw, expected);
        }
    }

    private static final List<Case> CASES = List.of(
        Case.of("ruta relativa dentro de la politica", "specs", "specs/feat/requirements.md", ALLOW),
        Case.of("ruta absoluta dentro de la politica", "specs", "{ROOT}/specs/feat/design.md", ALLOW),
        Case.of("la carpeta de la politica en si", "specs", "specs", ALLOW),
        Case.of("politica con barra final", "specs/", "specs/feat/tasks.md", ALLOW),
        Case.of("fichero suelto permitido", "README.md", "README.md", ALLOW),
        Case.of("dos rutas en la politica, la segunda", "specs docs", "docs/a.md", ALLOW),
        Case.of("fuera de la politica", "specs", "README.md", DENY),
        Case.of("prefijo parecido pero otra carpeta", "specs", "specsx/a.md", DENY),
        Case.of("punto punto en la ruta", "specs", "specs/../README.md", DENY),
        Case.of("punto punto que seguiria dentro", "specs", "specs/feat/../a.md", DENY),
        Case.of("ruta absoluta fuera de la raiz", "specs", "{OUTSIDE}/specs/x.md", DENY),
        Case.of("sin file_path", "specs", "", DENY),
        new Case("entrada que no es JSON", "specs", "", "", "", "esto no es json", DENY),
        new Case("entrada vacia", "specs", "", "", "", "", DENY),
        Case.of("agente docs puede escribir el README", "specs", "README.md", ALLOW).agent("docs"),
        Case.of("agente docs-readme puede escribir el README", "specs", "README.md", ALLOW).agent("docs-readme"),
        Case.of("agente docs no puede escribir en specs", "specs", "specs/a.md", DENY).agent("docs"),
        Case.of("otro agente sigue la politica recibida", "specs", "specs/a.md", ALLOW).agent("sdd-design"),
        Case.of("raiz detectada subiendo desde un subdirectorio", "specs", "specs/x.md", ALLOW).cwd("specs/feat"),
        Case.of("subdirectorio no permitido sigue denegado", "specs", "src/x.md", DENY).cwd("specs/feat"),
        Case.of("sin cwd se usa el directorio del proceso", "specs", "specs/x.md", ALLOW).cwd(null));

    private interface Runner {
        String name();

        /** Ruta en el formato del sistema que ejecuta el script. */
        String path(Path path) throws IOException, InterruptedException;

        int run(Path script, String policy, Path workDir, String stdin) throws IOException, InterruptedException;

        /** Contenido del recurso y nombre con el que se guarda. */
        TargetOs variant();
    }

    @BeforeEach
    void setUp() throws IOException {
        root = Files.createDirectories(tmp.resolve("proyecto"));
        Files.createDirectories(root.resolve(".sdd"));
        Files.writeString(root.resolve(".sdd/project.md"), "## Nombre del proyecto\n\nx\n");
        Files.createDirectories(root.resolve("specs/feat"));
        outside = Files.createDirectories(tmp.resolve("otro"));
    }

    // --- ejecutores ---

    private static List<String> output(List<String> command, Path workDir, String stdin)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).directory(workDir.toFile()).redirectErrorStream(true).start();
        process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().close();
        String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(60, TimeUnit.SECONDS), "El script no termino: " + command);
        List<String> lines = new ArrayList<>(List.of(text.split("\\R")));
        lines.add(0, String.valueOf(process.exitValue()));
        return lines;
    }

    private static boolean works(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            process.getOutputStream().close();
            process.getInputStream().readAllBytes();
            return process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch(IOException | InterruptedException e) {
            if(e instanceof InterruptedException)
                Thread.currentThread().interrupt();
            return false;
        }
    }

    private static Runner bat() {
        return new Runner() {
            @Override public String name() { return ".bat"; }
            @Override public TargetOs variant() { return TargetOs.WINDOWS; }
            @Override public String path(Path path) { return path.toAbsolutePath().toString(); }

            @Override
            public int run(Path script, String policy, Path workDir, String stdin) throws IOException, InterruptedException {
                List<String> command = new ArrayList<>(List.of("cmd.exe", "/d", "/c", script.toString()));
                command.addAll(List.of(policy.split(" ")));
                return Integer.parseInt(output(command, workDir, stdin).get(0));
            }
        };
    }

    private static Runner sh() {
        return new Runner() {
            @Override public String name() { return ".sh"; }
            @Override public TargetOs variant() { return TargetOs.LINUX; }

            // En Windows (Git Bash) las rutas que ve el script son POSIX: /c/Users/...
            @Override
            public String path(Path path) throws IOException, InterruptedException {
                if(!WINDOWS)
                    return path.toAbsolutePath().toString();
                List<String> out = output(List.of(BASH, "-c", "cygpath -u \"$1\"", "_", path.toAbsolutePath().toString()),
                    path.toAbsolutePath().getParent() == null ? path : path.toAbsolutePath().getParent(), "");
                return out.get(1);
            }

            @Override
            public int run(Path script, String policy, Path workDir, String stdin) throws IOException, InterruptedException {
                List<String> command = new ArrayList<>(List.of(BASH, path(script)));
                command.addAll(List.of(policy.split(" ")));
                return Integer.parseInt(output(command, workDir, stdin).get(0));
            }
        };
    }

    private boolean shAvailable() {
        if(BASH == null)
            return false;
        String check = WINDOWS ? "command -v jq && command -v cygpath" : "command -v jq";
        return works(List.of(BASH, "-c", check));
    }

    private static boolean batAvailable() {
        return WINDOWS && works(List.of("cmd.exe", "/d", "/c", "where", "jq"));
    }

    // --- el test ---

    private String stdin(Case c, Runner runner) throws IOException, InterruptedException {
        if(c.raw() != null)
            return c.raw();

        Map<String, Object> input = new LinkedHashMap<>();
        Map<String, Object> tool = new LinkedHashMap<>();
        if(!c.file().isEmpty()) {
            tool.put("file_path", c.file()
                .replace("{ROOT}", runner.path(root))
                .replace("{OUTSIDE}", runner.path(outside)));
        }
        input.put("tool_input", tool);
        if(!c.agent().isEmpty())
            input.put("agent_type", c.agent());
        if(c.cwd() != null) {
            Path dir = c.cwd().isEmpty() ? root : root.resolve(c.cwd());
            input.put("cwd", runner.path(dir));
        }
        return JSON.writeValueAsString(input);
    }

    private List<String> mismatches(Runner runner) throws IOException, InterruptedException {
        Path script = tmp.resolve("hook").resolve(HookResource.fileName(runner.variant()));
        Files.createDirectories(script.getParent());
        Files.write(script, HookResource.content(runner.variant()));

        List<String> failures = new ArrayList<>();
        for(Case c : CASES) {
            int actual = runner.run(script, c.policy(), root, stdin(c, runner));
            if(actual != c.expected())
                failures.add(runner.name() + " | " + c.name() + " | esperado " + c.expected() + " y salio " + actual);
        }
        return failures;
    }

    @Test
    void theBatVariantGivesTheExpectedResultForEveryCase() throws Exception {
        assumeTrue(batAvailable(), "Solo en Windows y con jq en el PATH");

        assertEquals(List.of(), mismatches(bat()));
    }

    @Test
    void theShVariantGivesTheExpectedResultForEveryCase() throws Exception {
        assumeTrue(shAvailable(), "Hace falta bash y jq (en Windows, Git Bash con cygpath)");

        assertEquals(List.of(), mismatches(sh()));
    }

    @Test
    void bothVariantsAgreeOnEveryCase() throws Exception {
        assumeTrue(batAvailable() && shAvailable(), "La paridad solo se puede comprobar con ambas variantes ejecutables");

        List<String> failures = new ArrayList<>(mismatches(bat()));
        failures.addAll(mismatches(sh()));

        assertEquals(List.of(), failures);
    }
}
