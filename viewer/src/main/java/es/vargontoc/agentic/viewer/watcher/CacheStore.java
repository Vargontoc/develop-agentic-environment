package es.vargontoc.agentic.viewer.watcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Caché de lecturas en memoria para {@code status}, {@code tree} y
 * {@code describe}. Se invalida tras escrituras o eventos de cambio externo.
 */
public final class CacheStore {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    /**
     * Devuelve el valor cacheado para la clave dada, o lo obtiene del
     * proveedor si no está presente.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Supplier<T> loader) {
        Object existing = cache.get(key);
        if (existing != null) {
            return (T) existing;
        }
        T value = loader.get();
        if (value != null) {
            cache.put(key, value);
        }
        return value;
    }

    /** Invalida una clave concreta. */
    public void invalidate(String key) {
        cache.remove(key);
    }

    /** Invalida todas las entradas que empiezan con el prefijo dado. */
    public void invalidatePrefix(String prefix) {
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /** Invalida toda la caché (por ejemplo, tras un cambio externo). */
    public void invalidateAll() {
        cache.clear();
    }

    /**
     * Invalida solo lo que puede haber cambiado tras una escritura en un
     * proyecto: su estado, su árbol y las descripciones de sus artefactos.
     */
    public void invalidateProject(String projectPath) {
        invalidate(statusKey(projectPath));
        invalidate(treeKey(projectPath));
        String project = projectPath == null ? "global" : projectPath;
        invalidatePrefix(DESCRIBE + ":" + project + ":");
    }

    /** Invalida un artefacto concreto y los datos de árbol y estado del proyecto. */
    public void invalidateArtifact(String artifactPath, String projectPath) {
        invalidate(describeKey(artifactPath, projectPath));
        invalidate(treeKey(projectPath));
        invalidate(statusKey(projectPath));
    }

    /** Número de entradas actualmente cacheadas. */
    public int size() {
        return cache.size();
    }

    /** Claves de utilidad para construir claves normalizadas. */
    public static final String STATUS = "status";
    public static final String TREE = "tree";
    public static final String DESCRIBE = "describe";

    /**
     * Construye una clave para {@code describe} a partir de la ruta del
     * artefacto y el proyecto.
     */
    public static String describeKey(String artifactPath, String projectPath) {
        return DESCRIBE + ":" + (projectPath == null ? "global" : projectPath) + ":" + artifactPath;
    }

    /**
     * Construye una clave para {@code status} a partir del proyecto.
     */
    public static String statusKey(String projectPath) {
        return STATUS + ":" + (projectPath == null ? "global" : projectPath);
    }

    /**
     * Construye una clave para {@code tree} a partir del proyecto.
     */
    public static String treeKey(String projectPath) {
        return TREE + ":" + (projectPath == null ? "global" : projectPath);
    }
}
