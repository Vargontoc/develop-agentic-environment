package es.vargontoc.agentic.viewer.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * El viewer espera el jar ejecutable del CLI en {@code cli/agentic-develop.jar}
 * dentro de su propio directorio. No hay rutas de desarrollo alternativas: si
 * falta, la aplicación debe mostrar un error y no continuar.
 */
public final class CliLocator {

    private static final String JAR_RELATIVE_PATH = "cli/agentic-develop.jar";

    private CliLocator() {}

    public static CliCommand resolve(Path viewerDir) {
        Path jar = locate(viewerDir);
        return new CliCommand(javaExecutable(), List.of("-jar", jar.toString()), jar);
    }

    static Path locate(Path viewerDir) {
        Path base = viewerDir.toAbsolutePath().normalize();
        Path bundled = base.resolve(JAR_RELATIVE_PATH).normalize();
        Path development = base.resolve("../cli/target/agentic-develop.jar").normalize();

        // En desarrollo, si el CLI se ha compilado después que la copia empaquetada,
        // se usa el jar nuevo para no arrastrar plantillas antiguas.
        if (Files.isRegularFile(development) && isNewer(development, bundled)) {
            return development;
        }
        if (Files.isRegularFile(bundled)) {
            return bundled;
        }
        if (Files.isRegularFile(development)) {
            return development;
        }
        throw new IllegalStateException(
            "No se encontró el CLI. Coloca agentic-develop.jar en " + bundled
                + " (carpeta cli junto al viewer).");
    }

    private static boolean isNewer(Path candidate, Path reference) {
        if (!Files.isRegularFile(reference)) {
            return true;
        }
        try {
            return Files.getLastModifiedTime(candidate).compareTo(Files.getLastModifiedTime(reference)) > 0;
        } catch (IOException exception) {
            return false;
        }
    }

    private static Path javaExecutable() {
        String executable = File.separatorChar == '\\' ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }
}
