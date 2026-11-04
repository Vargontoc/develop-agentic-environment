package es.vargontoc.agentic.develop.cli.domain.team;

import java.util.List;

import es.vargontoc.agentic.develop.cli.domain.Technician;

/**
 * Un módulo registrado en {@code .sdd/team.md} con el patrón {@code - modulo | id del mapa (o -) | rutas separadas por espacio}.
 *
 * @param mapId id del mapa de arquitectura; null si consta {@code -}
 * @param paths rutas de código del módulo; vacío si consta {@code -}
 * @param line  línea (desde 1) donde está registrado
 */
public record ModuleEntry(String name, String mapId, List<String> paths, int line) {

    public ModuleEntry {
        paths = List.copyOf(paths);
    }

    /** Nombres de los especialistas del módulo, por convención {@code <rol>-<modulo>}. */
    public List<String> specialists() {
        return List.of(Technician.values()).stream().map(t -> t.slug() + "-" + name).toList();
    }
}
