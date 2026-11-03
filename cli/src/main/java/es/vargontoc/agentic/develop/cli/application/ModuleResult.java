package es.vargontoc.agentic.develop.cli.application;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de lo que hizo la creación de un módulo, para que la UI lo muestre.
 */
public record ModuleResult(List<String> created, List<String> skipped, List<String> warnings) {

    public ModuleResult() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
}
