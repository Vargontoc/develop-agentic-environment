package es.vargontoc.agentic.develop.cli.domain.format;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;

/**
 * Placeholders del cuerpo compartido de los agentes de proyecto (DEC-007). El cuerpo compartido los conserva sin
 * resolver; cada artefacto los resuelve con su módulo y su proveedor, así un mismo cuerpo sirve a todos los módulos y
 * proveedores del grupo.
 * <ul>
 *   <li>{@code {module}}: módulo del especialista (el router no tiene módulo y lo deja intacto)</li>
 *   <li>{@code {slug-provider}}: carpeta del proveedor en el proyecto, p. ej. {@code .claude}</li>
 *   <li>{@code {slug-path-agents}}: carpeta de agentes del proveedor, p. ej. {@code .claude/agents}</li>
 * </ul>
 */
public final class BodyPlaceholders {

    public static final String MODULE = "{module}";

    private BodyPlaceholders() {}

    /** @param module módulo del artefacto, o null si no tiene (routers y artefactos globales) */
    public static String resolve(String body, ProviderType provider, String module) {
        String out = module == null ? body : body.replace(MODULE, module);
        if(provider.supportsProject())
            out = out.replace("{slug-path-agents}", provider.projectDir() + "/agents")
                     .replace("{slug-provider}", provider.projectDir());
        return out;
    }
}
