package es.vargontoc.agentic.develop.cli.application;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de una operación de escritura (crear módulo, configurar proveedor): qué se creó, qué se omitió por existir
 * ya y qué avisos hubo. Los servicios lo devuelven y es el adaptador quien decide cómo mostrarlo.
 */
public record OperationResult(List<String> created, List<String> skipped, List<String> warnings) {

    public OperationResult() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
}
