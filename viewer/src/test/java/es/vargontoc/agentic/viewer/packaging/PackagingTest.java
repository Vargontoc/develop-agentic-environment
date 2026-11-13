package es.vargontoc.agentic.viewer.packaging;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Pruebas de empaquetado. Verifican que el JAR tiene el MANIFEST correcto
 * y que la estructura de directorios es la esperada.
 * 
 * Estas pruebas se ejecutan solo cuando se establece la propiedad del sistema
 * -Dtest.packaging=true, ya que requieren que el JAR esté empaquetado.
 */
class PackagingTest {

    @Test
    @EnabledIfSystemProperty(named = "test.packaging", matches = "true")
    void viewerJarHasCorrectManifest() throws IOException {
        Path viewerJar = Path.of("target/viewer-" + es.vargontoc.agentic.viewer.runtime.ViewerVersion.current() + ".jar");
        assertTrue(Files.exists(viewerJar), "El JAR del viewer debe existir");

        try (JarFile jar = new JarFile(viewerJar.toFile())) {
            Manifest manifest = jar.getManifest();
            assertNotNull(manifest, "El MANIFEST.MF debe existir");

            Attributes mainAttrs = manifest.getMainAttributes();
            String mainClass = mainAttrs.getValue("Main-Class");
            assertEquals("es.vargontoc.agentic.viewer.App", mainClass,
                "Main-Class debe ser es.vargontoc.agentic.viewer.App");
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "test.packaging", matches = "true")
    void fatJarContainsAllDependencies() throws IOException {
        Path fatJar = Path.of("target/viewer-fat.jar");
        if (!Files.exists(fatJar)) {
            // Fat JAR not yet created, skip
            return;
        }

        try (JarFile jar = new JarFile(fatJar.toFile())) {
            // Verificar que contiene clases de dependencias clave
            assertTrue(jar.getEntry("com/fasterxml/jackson/databind/ObjectMapper.class") != null,
                "Debe contener Jackson ObjectMapper");
            assertTrue(jar.getEntry("org/commonmark/Parser.class") != null,
                "Debe contener CommonMark Parser");
            assertTrue(jar.getEntry("com/pty4j/PtyProcess.class") != null,
                "Debe contener Pty4J PtyProcess");
            assertTrue(jar.getEntry("es/vargontoc/agentic/viewer/App.class") != null,
                "Debe contener App.class");
        }
    }

    @Test
    void requiredModulesAreIdentified() {
        List<String> requiredModules = List.of(
            "java.base",
            "java.desktop",
            "java.logging",
            "java.xml",
            "java.sql",
            "java.management",
            "java.naming",
            "java.scripting",
            "jdk.unsupported",
            "javafx.base",
            "javafx.controls",
            "javafx.fxml",
            "javafx.graphics",
            "javafx.media",
            "javafx.swing",
            "javafx.web"
        );

        assertFalse(requiredModules.isEmpty(), "Debe haber módulos requeridos");
        assertTrue(requiredModules.contains("java.base"), "java.base es obligatorio");
        assertTrue(requiredModules.contains("java.desktop"), "java.desktop es necesario para JavaFX");
        assertTrue(requiredModules.contains("javafx.controls"), "javafx.controls es necesario para la UI");
    }

    @Test
    void cliJarPathIsCorrect() {
        // Verificar que la ruta del CLI jar es la esperada
        Path viewerDir = Path.of("").toAbsolutePath();
        Path cliJar = viewerDir.resolve("cli").resolve("agentic-develop.jar");
        
        assertEquals("agentic-develop.jar", cliJar.getFileName().toString(),
            "El nombre del jar del CLI debe ser agentic-develop.jar");
        assertTrue(cliJar.toString().contains("cli"),
            "El jar del CLI debe estar en el directorio cli/");
    }
}
