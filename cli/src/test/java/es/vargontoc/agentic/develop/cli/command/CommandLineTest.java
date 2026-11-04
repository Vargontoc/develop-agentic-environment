package es.vargontoc.agentic.develop.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.support.Console;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

class CommandLineTest {

    private static final JsonMapper JSON = JsonMapperFactory.create();

    @TempDir
    Path tmp;

    /** Comando de pruebas: configurable en lo que hace al ejecutarse. */
    private record Stub(String name, List<OptionSpec> options, java.util.function.Function<ParsedArgs, Object> body) implements Command {
        @Override public String description() { return "Comando de prueba " + name; }
        @Override public Object execute(ParsedArgs args) { return body.apply(args); }
    }

    private record Output(int exit, String raw) {
        Map<?, ?> json() {
            return JSON.readValue(raw, Map.class);
        }

        Map<?, ?> firstError() {
            return (Map<?, ?>) ((List<?>) json().get("errors")).get(0);
        }
    }

    /** Su getter falla al serializarse. */
    public static final class Unserializable {
        public String getValue() {
            throw new IllegalStateException("no se puede serializar");
        }
    }

    private static CommandRegistry registry() {
        Supplier<Object> unserializable = Unserializable::new;
        return new CommandRegistry()
            .register(new Stub("echo", List.of(OptionSpec.value("name", true, "Nombre"), OptionSpec.flag("loud", "En alto")),
                a -> Map.of("name", a.required("name"), "loud", a.flag("loud"))))
            .register(new Stub("domain-fail", List.of(), a -> { throw new DomainException(AppErrorCode.MODULE_NAME_TOO_SHORT); }))
            .register(new Stub("path-fail", List.of(), a -> { throw new PathDomainException(AppErrorCode.PROJECT_PATH_NOT_FOUND, "/no/existe"); }))
            .register(new Stub("internal-fail", List.of(), a -> { throw new DomainException(AppErrorCode.TEXT_WRITE_FAILED); }))
            .register(new Stub("boom", List.of(), a -> { throw new IllegalStateException("detalle secreto de la traza"); }))
            .register(new Stub("bad-data", List.of(), a -> unserializable.get()));
    }

    private Output run(String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int exit = new CommandLine(registry(), () -> "9.9.9-TEST").run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return new Output(exit, buffer.toString(StandardCharsets.UTF_8));
    }

    // --- éxito y forma del envelope ---

    @Test
    void successfulCommandWritesOneJsonDocumentWithTheEnvelopeFields() {
        Output out = run("echo", "--name", "alfa", "--loud");

        assertEquals(0, out.exit());
        assertEquals(1, out.raw().strip().split("\\R").length);
        Map<?, ?> json = out.json();
        assertEquals(List.of("schemaVersion", "command", "ok", "data", "errors"), List.copyOf(json.keySet()));
        assertEquals(1, json.get("schemaVersion"));
        assertEquals("echo", json.get("command"));
        assertEquals(true, json.get("ok"));
        assertEquals(Map.of("name", "alfa", "loud", true), json.get("data"));
        assertEquals(List.of(), json.get("errors"));
    }

    @Test
    void optionsAcceptTheEqualsForm() {
        assertEquals(Map.of("name", "a b", "loud", false), run("echo", "--name=a b").json().get("data"));
    }

    @Test
    void commandsPrintNothingElseToTheProcessOutput() {
        String stdout = Console.capturedOut(() -> run("echo", "--name", "x"));
        String stdoutOnError = Console.capturedOut(() -> run("boom"));

        assertEquals("", stdout);
        assertEquals("", stdoutOnError);
    }

    // --- ayuda y versión ---

    @Test
    void helpListsCommandsAndTheirOptionsWithoutWritingFiles() throws IOException {
        Path file = Files.writeString(tmp.resolve("x.txt"), "x");

        Output out = run("--help");

        assertEquals(0, out.exit());
        Map<?, ?> json = out.json();
        assertEquals("help", json.get("command"));
        List<?> commands = (List<?>) ((Map<?, ?>) json.get("data")).get("commands");
        assertEquals(List.of("bad-data", "boom", "domain-fail", "echo", "internal-fail", "path-fail"),
            commands.stream().map(c -> ((Map<?, ?>) c).get("name")).toList());
        Map<?, ?> echo = (Map<?, ?>) commands.get(3);
        assertEquals(List.of("name", "loud"), ((List<?>) echo.get("options")).stream().map(o -> ((Map<?, ?>) o).get("name")).toList());
        try(Stream<Path> files = Files.list(tmp)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    @Test
    void helpOfOneCommandListsOnlyThatCommand() {
        Output out = run("echo", "--help");

        List<?> commands = (List<?>) ((Map<?, ?>) out.json().get("data")).get("commands");
        assertEquals(0, out.exit());
        assertEquals(1, commands.size());
        assertEquals("echo", ((Map<?, ?>) commands.get(0)).get("name"));
        assertEquals(0, run("-h").exit());
    }

    @Test
    void versionIsReported() {
        Output out = run("--version");

        assertEquals(0, out.exit());
        assertEquals("version", out.json().get("command"));
        assertEquals(Map.of("version", "9.9.9-TEST"), out.json().get("data"));
    }

    // --- uso incorrecto: código 2 ---

    @Test
    void unknownCommandIsAUsageErrorThatKeepsWhatWasTyped() {
        Output out = run("inexistente");

        assertEquals(2, out.exit());
        assertEquals(false, out.json().get("ok"));
        assertNull(out.json().get("data"));
        assertEquals("inexistente", out.json().get("command"));
        assertEquals("usage.unknown_command", out.firstError().get("code"));
        assertFalse(out.firstError().get("message").toString().isBlank());
    }

    @Test
    void everyKindOfBadUsageIsCodeTwoWithItsOwnStableCode() {
        assertUsage("usage.unknown_option", run("echo", "--name", "a", "--nada"));
        assertUsage("usage.unknown_option", run("-x"));
        assertUsage("usage.missing_option", run("echo"));
        assertUsage("usage.missing_option", run("echo", "--loud"));
        assertUsage("usage.missing_value", run("echo", "--name"));
        assertUsage("usage.missing_value", run("echo", "--name", "--loud"));
        assertUsage("usage.missing_value", run("echo", "--name="));
        assertUsage("usage.duplicate_option", run("echo", "--name", "a", "--name", "b"));
        assertUsage("usage.duplicate_option", run("echo", "--name", "a", "--loud", "--loud"));
        assertUsage("usage.unexpected_argument", run("echo", "--name", "a", "sobrante"));
        assertUsage("usage.unexpected_argument", run("echo", "--name", "a", "--loud=si"));
        assertUsage("usage.unexpected_argument", run("--version", "extra"));
        assertUsage("usage.unexpected_argument", run("--help", "extra"));
    }

    private static void assertUsage(String code, Output out) {
        assertEquals(2, out.exit(), out.raw());
        assertEquals(false, out.json().get("ok"));
        assertEquals(code, out.firstError().get("code"), out.raw());
    }

    // --- errores de dominio e internos: códigos 1 y 3 ---

    @Test
    void domainErrorsAreCodeOneWithTheirStableCodeAndAMessage() {
        Output out = run("domain-fail");

        assertEquals(1, out.exit());
        assertEquals("domain-fail", out.json().get("command"));
        assertEquals("validation.module.name_too_short", out.firstError().get("code"));
        assertEquals("El nombre del módulo debe tener al menos 3 caracteres", out.firstError().get("message"));
        assertNull(out.firstError().get("path"));
    }

    @Test
    void errorsThatCarryAPathExposeIt() {
        Output out = run("path-fail");

        assertEquals(1, out.exit());
        assertEquals("not_found.project.path", out.firstError().get("code"));
        assertEquals("/no/existe", out.firstError().get("path"));
    }

    @Test
    void internalErrorsAreCodeThree() {
        Output out = run("internal-fail");

        assertEquals(3, out.exit());
        assertEquals("internal.text.write", out.firstError().get("code"));
    }

    @Test
    void unexpectedFailuresAreCodeThreeAndNeverLeakTheTraceIntoTheData() {
        Output out = run("boom");

        assertEquals(3, out.exit());
        assertEquals("internal.unexpected", out.firstError().get("code"));
        assertFalse(out.raw().contains("detalle secreto"), out.raw());
        assertFalse(out.raw().contains("IllegalStateException"), out.raw());
        assertFalse(out.raw().contains("\tat "), out.raw());
    }

    @Test
    void dataThatCannotBeSerializedIsAnInternalErrorWithAValidEnvelope() {
        Output out = run("bad-data");

        assertEquals(3, out.exit());
        assertEquals(false, out.json().get("ok"));
        assertEquals("internal.unexpected", out.firstError().get("code"));
    }

    // --- piezas sueltas ---

    @Test
    void everyApplicationErrorCodeHasAReadableMessage() {
        for(AppErrorCode code : AppErrorCode.values())
            assertFalse(ErrorMessages.of(code).isBlank(), code.name());
    }

    @Test
    void usageCodesAreStableAndNamespaced() {
        for(UsageError error : UsageError.values()) {
            assertTrue(error.value().startsWith("usage."), error.name());
            assertFalse(error.message().isBlank(), error.name());
        }
    }

    @Test
    void versionComesFromThePomThroughTheFilteredResource() {
        String version = Version.current();

        assertFalse(version.startsWith("${"), version);
        assertFalse(version.equals("unknown"));
    }

    @Test
    void exitCodesMatchTheContract() {
        assertEquals(List.of(0, 1, 2, 3), List.of(ExitCode.values()).stream().map(ExitCode::value).toList());
    }
}
