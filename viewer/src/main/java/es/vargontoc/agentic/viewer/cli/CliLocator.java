package es.vargontoc.agentic.viewer.cli;

import java.io.File;
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
        Path jar = base.resolve(JAR_RELATIVE_PATH).normalize();
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException(
                "No se encontró el CLI. Coloca agentic-develop.jar en " + jar
                    + " (carpeta cli junto al viewer).");
        }
        return jar;
    }

    private static Path javaExecutable() {
        String executable = File.separatorChar == '\\' ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }
}
