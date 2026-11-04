package es.vargontoc.agentic.develop.cli.domain.artifact;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;

/**
 * Un artefacto de la aplicación esperado en un proveedor y ámbito.
 *
 * @param relativePath ruta bajo la carpeta del proveedor, siempre con {@code /} (p. ej. {@code agents/sdd-agent.md})
 * @param format       formato del agente; null para skills y perfiles
 * @param resource     recurso del jar del que se instala: cabecera del agente, directorio de la skill o fichero del perfil
 * @param bodyResource recurso del cuerpo compartido o plantilla del agente; null para skills y perfiles
 */
public record CatalogEntry(
        ProviderType provider,
        ArtifactScope scope,
        ArtifactKind kind,
        String name,
        String relativePath,
        ArtifactFormat format,
        String resource,
        String bodyResource) {

    /** Las skills se instalan, describen y propagan como directorio completo. */
    public boolean isDirectory() {
        return kind == ArtifactKind.SKILL;
    }
}
