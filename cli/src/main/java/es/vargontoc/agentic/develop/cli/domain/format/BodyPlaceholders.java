package es.vargontoc.agentic.develop.cli.domain.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;

/**
 * Placeholders del cuerpo compartido de los agentes de proyecto (DEC-007). El cuerpo compartido los conserva sin
 * resolver; cada artefacto los resuelve con su módulo y su proveedor, así un mismo cuerpo sirve a todos los módulos y
 * proveedores del grupo.
 * <ul>
 *   <li>{@code {module}}: módulo del especialista (el router no tiene módulo y lo deja intacto)</li>
 *   <li>{@code {slug-provider}}: carpeta del proveedor en el proyecto, p. ej. {@code .claude}; si usa varias, se unen</li>
 *   <li>{@code {slug-path-agents}}: carpeta de agentes del proveedor, p. ej. {@code .claude/agents}
 *       ({@code .agents/agents} en Gemini)</li>
 *   <li>{@code {slug-path-specialist:<rol>}}: ruta del especialista del rol (con {@code <modulo>} sin resolver), que
 *       varía por proveedor: {@code .claude/agents/analista-<modulo>.md}, {@code .codex/agents/analista_<modulo>.toml}
 *       o {@code .agents/agents/analista-<modulo>/agent.md}</li>
 * </ul>
 */
public final class BodyPlaceholders {

    public static final String MODULE = "{module}";

    private static final Pattern SPECIALIST = Pattern.compile("\\{slug-path-specialist:([a-z]+)}");

    private BodyPlaceholders() {}

    /** @param module módulo del artefacto, o null si no tiene (routers y artefactos globales) */
    public static String resolve(String body, ProviderType provider, String module) {
        String out = module == null ? body : body.replace(MODULE, module);
        out = SPECIALIST.matcher(out).replaceAll(m -> Matcher.quoteReplacement(specialistPath(provider, m.group(1))));
        // Un proveedor puede usar varias carpetas: "`{slug-provider}/`" da "`.agents/`, `.codex/`"
        return out.replace("{slug-path-agents}", provider.projectAgentsDir())
                  .replace("{slug-provider}", String.join("/`, `", provider.projectDirs()));
    }

    private static String specialistPath(ProviderType provider, String role) {
        String dir = provider.projectAgentsDir();
        return switch(provider) {
            case CODEX -> dir + "/" + role + "_<modulo>.toml";
            case GEMINI -> dir + "/" + role + "-<modulo>/agent.md";
            case CLAUDE, OPENCODE -> dir + "/" + role + "-<modulo>.md";
        };
    }
}
