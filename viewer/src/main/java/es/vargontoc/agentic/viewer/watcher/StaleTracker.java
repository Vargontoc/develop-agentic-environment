package es.vargontoc.agentic.viewer.watcher;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mantiene un conjunto de rutas que han cambiado desde la última recarga.
 * Permite consultar si un nodo del árbol (por su ruta) está obsoleto.
 */
public final class StaleTracker {

    private final Set<String> stalePaths = Collections.synchronizedSet(new HashSet<>());
    private boolean anyStale = false;

    /**
     * Registra las rutas cambiadas como obsoletas.
     */
    public void markStale(List<Path> changedPaths) {
        for (Path path : changedPaths) {
            stalePaths.add(path.toAbsolutePath().normalize().toString());
        }
        if (!changedPaths.isEmpty()) {
            anyStale = true;
        }
    }

    /**
     * Indica si la ruta dada (o un ancestro suyo) está marcada como obsoleta.
     */
    public boolean isStale(String nodePath) {
        if (nodePath == null) {
            return false;
        }
        String normalized = Path.of(nodePath).toAbsolutePath().normalize().toString();
        if (stalePaths.contains(normalized)) {
            return true;
        }
        // Check if any ancestor directory is stale
        Path current = Path.of(normalized).getParent();
        while (current != null) {
            if (stalePaths.contains(current.toString())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Indica si hay algún nodo obsoleto.
     */
    public boolean isAnyStale() {
        return anyStale;
    }

    /**
     * Limpia todas las marcas de obsolescencia (tras una recarga).
     */
    public void clear() {
        stalePaths.clear();
        anyStale = false;
    }

    /**
     * Número de rutas obsoletas actualmente rastreadas.
     */
    public int size() {
        return stalePaths.size();
    }
}
