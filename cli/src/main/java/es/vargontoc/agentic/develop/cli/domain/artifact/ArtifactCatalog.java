package es.vargontoc.agentic.develop.cli.domain.artifact;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;

/**
 * Los artefactos que la aplicación espera encontrar en cada proveedor. Es la única fuente de pertenencia:
 * lo que no figura aquí (agentes propios del usuario, recursos del jar sin instalar) no es un artefacto de la aplicación.
 */
public interface ArtifactCatalog {

    /**
     * Artefactos esperados. En el ámbito de proyecto, los especialistas dependen de los módulos registrados.
     */
    List<CatalogEntry> entries(ProviderType provider, ArtifactScope scope, Collection<String> modules);

    default List<CatalogEntry> entries(ProviderType provider, ArtifactScope scope) {
        return entries(provider, scope, List.of());
    }

    /** Identifica un artefacto por su ruta lógica ({@code agents/x.md}, {@code skills/x}). */
    default Optional<CatalogEntry> identify(ProviderType provider, ArtifactScope scope, String relativePath,
                                            Collection<String> modules) {
        String normalized = relativePath.replace('\\', '/');
        return entries(provider, scope, modules).stream()
            .filter(e -> e.relativePath().equals(normalized))
            .findFirst();
    }

    /**
     * Identifica un artefacto instalado por su ubicación física relativa a la base del proveedor. Un fichero dentro de
     * una skill (SKILL.md, apoyos) identifica la skill entera.
     */
    default Optional<CatalogEntry> identifyAt(ProviderType provider, ArtifactScope scope, String location,
                                              Collection<String> modules) {
        String normalized = location.replace('\\', '/');
        return entries(provider, scope, modules).stream()
            .filter(e -> e.location().equals(normalized)
                || e.isDirectory() && normalized.startsWith(e.location() + "/"))
            .findFirst();
    }
}
