package es.vargontoc.agentic.develop.cli.domain.metadata;

import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;

/**
 * Identificador del grupo lógico de un artefacto del catálogo. Es una función de la entrada del catálogo (nunca se
 * infiere del contenido, DEC-013), así que sirve igual para artefactos recién generados y para los que ya existían.
 * <ul>
 *   <li>agente global: {@code global/agent/<nombre>} (el mismo en todos los proveedores)</li>
 *   <li>router de proyecto: {@code project/router/<técnico>}</li>
 *   <li>especialista de proyecto: {@code project/specialist/<técnico>} (el mismo en todos los módulos)</li>
 *   <li>skill: {@code <ámbito>/skill/<nombre>}; perfil: {@code global/profile/<nombre>}</li>
 * </ul>
 */
public final class GroupIds {

    private GroupIds() {}

    public static String of(CatalogEntry entry) {
        String scope = entry.scope() == ArtifactScope.GLOBAL ? "global" : "project";
        if(entry.kind() == ArtifactKind.SKILL)
            return scope + "/skill/" + entry.name();
        if(entry.kind() == ArtifactKind.PROFILE)
            return scope + "/profile/" + entry.name();
        if(entry.scope() == ArtifactScope.GLOBAL)
            return "global/agent/" + entry.name().replace('_', '-');
        String role = entry.module() == null ? "router" : "specialist";
        return "project/" + role + "/" + entry.technician().slug();
    }
}
