package es.vargontoc.agentic.viewer.cli;

import java.time.Duration;
import java.util.List;

/**
 * Abstracción mínima para ejecutar el CLI. Permite probar el cliente sin
 * arrancar procesos reales.
 */
@FunctionalInterface
public interface CliRunner {

    CliProcess.CliResult run(List<String> args, String stdin, Duration timeout);

    default CliProcess.CliResult run(List<String> args) {
        return run(args, null, Duration.ofSeconds(15));
    }
}
