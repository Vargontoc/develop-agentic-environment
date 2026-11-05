package es.vargontoc.agentic.develop.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Reglas de capas comprobadas sobre el código fuente: los servicios no imprimen ni dependen de la interfaz de
 * usuario; solo el adaptador del menú ({@code ui}) y el arranque ({@code App}) escriben en la consola.
 */
class LayersTest {

    private static final Path SOURCES = Path.of("src/main/java/es/vargontoc/agentic/develop/cli");
    private static final Pattern CONSOLE = Pattern.compile("System\\.(out|err)|printStackTrace");
    private static final List<String> HEADLESS = List.of("domain", "application", "infrastructure", "command");

    private static List<Path> sources(String layer) throws IOException {
        try(Stream<Path> files = Files.walk(SOURCES.resolve(layer))) {
            return files.filter(f -> f.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void servicesAndAdaptersOtherThanTheMenuNeverUseTheConsole() throws IOException {
        for(String layer : HEADLESS) {
            for(Path file : sources(layer)) {
                String code = withoutComments(Files.readString(file));
                assertEquals(false, CONSOLE.matcher(code).find(), file + " usa la consola");
            }
        }
    }

    @Test
    void onlyTheCommandRegistryDefaultsToStandardInput() throws IOException {
        for(String layer : HEADLESS) {
            for(Path file : sources(layer)) {
                boolean usesIt = withoutComments(Files.readString(file)).contains("System.in");
                assertEquals(file.getFileName().toString().equals("Commands.java"), usesIt, file.toString());
            }
        }
    }

    @Test
    void theInnerLayersDoNotDependOnTheMenu() throws IOException {
        for(String layer : List.of("domain", "application", "infrastructure")) {
            for(Path file : sources(layer)) {
                assertTrue(!Files.readString(file).contains("cli.ui."), file + " depende de la interfaz de usuario");
            }
        }
    }

    @Test
    void theDomainDoesNotDependOnApplicationOrInfrastructure() throws IOException {
        for(Path file : sources("domain")) {
            String code = Files.readString(file);
            assertTrue(!code.contains("cli.application.") && !code.contains("cli.infrastructure."),
                file + " depende de capas externas");
        }
    }

    private static String withoutComments(String code) {
        return code.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }
}
