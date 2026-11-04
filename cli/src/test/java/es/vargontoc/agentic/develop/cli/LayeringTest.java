package es.vargontoc.agentic.develop.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Reglas de capas que el compilador no comprueba: ni el dominio ni la aplicación escriben en la consola
 * ni terminan el proceso (DEC-001); solo lo hacen los adaptadores de entrada.
 */
class LayeringTest {

    private static final Path BASE = Path.of("src/main/java/es/vargontoc/agentic/develop/cli");
    private static final Pattern FORBIDDEN = Pattern.compile("System\\s*\\.\\s*(out|err|exit|in)\\b|\\.printStackTrace\\s*\\(|Runtime\\s*\\.\\s*getRuntime\\(\\)\\s*\\.\\s*(exit|halt)");

    private static List<String> violations(String layer) throws IOException {
        try(Stream<Path> files = Files.walk(BASE.resolve(layer))) {
            return files.filter(f -> f.toString().endsWith(".java"))
                .filter(LayeringTest::hasForbiddenCall)
                .map(f -> BASE.relativize(f).toString())
                .sorted()
                .toList();
        }
    }

    private static boolean hasForbiddenCall(Path file) {
        try {
            return Files.readAllLines(file).stream()
                .filter(line -> !line.trim().startsWith("//") && !line.trim().startsWith("*"))
                .anyMatch(line -> FORBIDDEN.matcher(line).find());
        } catch(IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void domainDoesNotTouchTheConsoleOrExitTheProcess() throws IOException {
        assertEquals(List.of(), violations("domain"));
    }

    @Test
    void applicationDoesNotTouchTheConsoleOrExitTheProcess() throws IOException {
        assertEquals(List.of(), violations("application"));
    }

    @Test
    void infrastructureDoesNotTouchTheConsoleOrExitTheProcess() throws IOException {
        assertEquals(List.of(), violations("infrastructure"));
    }

    @Test
    void domainDoesNotDependOnOtherLayers() throws IOException {
        try(Stream<Path> files = Files.walk(BASE.resolve("domain"))) {
            List<String> offenders = files.filter(f -> f.toString().endsWith(".java"))
                .filter(LayeringTest::importsOuterLayer)
                .map(f -> BASE.relativize(f).toString())
                .sorted()
                .toList();
            assertEquals(List.of(), offenders);
        }
    }

    private static boolean importsOuterLayer(Path file) {
        try {
            return Files.readAllLines(file).stream().anyMatch(line -> line.startsWith("import es.vargontoc.agentic.develop.cli.")
                && (line.contains(".application.") || line.contains(".infrastructure.") || line.contains(".ui.")));
        } catch(IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
