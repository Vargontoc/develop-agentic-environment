package es.vargontoc.agentic.viewer.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Ejecutable y argumentos fijos para lanzar el CLI. En producción los argumentos
 * fijos son {@code -jar <ruta>}; en pruebas pueden ser {@code -cp <ruta> <clase>}.
 */
public record CliCommand(Path javaExecutable, List<String> prefixArgs, Path cliJar) {

    public CliCommand {
        prefixArgs = List.copyOf(prefixArgs);
    }

    public List<String> command(List<String> args) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable.toString());
        command.add("-Dstdout.encoding=UTF-8");
        command.add("-Dstderr.encoding=UTF-8");
        command.addAll(prefixArgs);
        command.addAll(args);
        return command;
    }
}
