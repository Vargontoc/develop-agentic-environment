package es.vargontoc.agentic.develop.cli.domain.team;

/**
 * Aviso al leer {@code team.md}. La lectura es tolerante: un aviso nunca impide devolver el resto de módulos.
 *
 * @param line línea (desde 1) afectada; 0 si no aplica a una línea concreta
 */
public record TeamWarning(Type type, int line) {

    public enum Type {
        /** No existe {@code .sdd/team.md} (o el proyecto no es un directorio). */
        TEAM_MISSING,
        /** El fichero existe pero no se puede leer. */
        TEAM_UNREADABLE,
        /** No hay sección de módulos. */
        SECTION_MISSING,
        /** Línea de la sección que no sigue el patrón. */
        MALFORMED_LINE,
        /** Módulo ya registrado antes; se conserva el primero. */
        DUPLICATE_MODULE
    }
}
