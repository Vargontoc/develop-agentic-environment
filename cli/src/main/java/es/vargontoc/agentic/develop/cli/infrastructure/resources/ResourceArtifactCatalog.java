package es.vargontoc.agentic.develop.cli.infrastructure.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactCatalog;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;

/**
 * Catálogo derivado de los recursos empaquetados: lo que el jar sabe instalar es lo que la aplicación espera encontrar.
 * Un agente solo es esperado en un proveedor si existe su cabecera para ese proveedor.
 * <p>
 * Las rutas del catálogo son lógicas e iguales en todos los proveedores ({@code agents/x.md}, {@code skills/x},
 * {@code profiles/x.toml}); dónde viven en disco lo decide {@code ProviderLayout}. Por proveedor solo cambia el
 * recurso de cabecera y el formato: los agentes de Codex son {@code .toml} y su router de proyecto una skill.
 */
public final class ResourceArtifactCatalog implements ArtifactCatalog {

    private static final String GLOBAL_AGENTS = "/global/agents";
    private static final String GLOBAL_SKILLS = "/global/skills";
    private static final String GLOBAL_PROFILE = "/global/profiles/sdd.config.toml";
    private static final String PROJECT_AGENTS = "/project/agents";
    private static final String PROJECT_SKILLS = "/project/skills";
    private static final String PROJECT_PROFILES = "/project/profiles";
    private static final String SHARED = "/shared/";
    private static final String PROFILE_SUFFIX = ".conf.toml";

    private record Key(ProviderType provider, ArtifactScope scope) {}

    // Los recursos no cambian durante la ejecución; lo que depende de módulos se calcula aparte
    private final Map<Key, List<CatalogEntry>> fixed = new ConcurrentHashMap<>();

    @Override
    public List<CatalogEntry> entries(ProviderType provider, ArtifactScope scope, Collection<String> modules) {
        List<CatalogEntry> base = fixed.computeIfAbsent(new Key(provider, scope), this::build);
        if(scope == ArtifactScope.GLOBAL || modules.isEmpty())
            return base;

        List<CatalogEntry> all = new ArrayList<>(base);
        all.addAll(specialists(provider, modules));
        return List.copyOf(all);
    }

    private List<CatalogEntry> build(Key key) {
        try {
            return key.scope() == ArtifactScope.GLOBAL ? global(key.provider()) : project(key.provider());
        } catch(IOException e) {
            throw new DomainException(AppErrorCode.RESOURCE_READ_FAILED, e);
        }
    }

    private List<CatalogEntry> global(ProviderType provider) throws IOException {
        List<CatalogEntry> entries = new ArrayList<>();
        ArtifactFormat format = provider == ProviderType.CODEX ? ArtifactFormat.CODEX_TOML : ArtifactFormat.MARKDOWN;

        String agentsDir = GLOBAL_AGENTS + "/" + provider.id();
        for(String file : ClasspathResources.listFiles(agentsDir).orElse(List.of())) {
            String name = baseName(file);
            entries.add(new CatalogEntry(provider, ArtifactScope.GLOBAL, ArtifactKind.AGENT, name, "agents/" + file, format,
                agentsDir + "/" + file, GLOBAL_AGENTS + SHARED + name.replace('_', '-') + "-body.md", null, null));
        }

        entries.addAll(skills(provider, ArtifactScope.GLOBAL, GLOBAL_SKILLS));

        if(provider == ProviderType.CODEX && ClasspathResources.exists(GLOBAL_PROFILE)) {
            entries.add(new CatalogEntry(provider, ArtifactScope.GLOBAL, ArtifactKind.PROFILE, "sdd.config",
                "profiles/sdd.config.toml", null, GLOBAL_PROFILE, null, null, null));
        }
        return List.copyOf(entries);
    }

    // En proyecto: skills y un router por técnico; los especialistas y sus perfiles dependen de los módulos
    private List<CatalogEntry> project(ProviderType provider) throws IOException {
        List<CatalogEntry> entries = new ArrayList<>(skills(provider, ArtifactScope.PROJECT, PROJECT_SKILLS));

        for(Technician technician : Technician.values()) {
            String name = technician.slug() + "-router";
            String header = routerHeader(provider, name);
            if(ClasspathResources.exists(header)) {
                entries.add(new CatalogEntry(provider, ArtifactScope.PROJECT, ArtifactKind.AGENT, name, "agents/" + name + ".md",
                    ArtifactFormat.MARKDOWN, header, PROJECT_AGENTS + SHARED + name + "-template.md", technician, null));
            }
        }
        return List.copyOf(entries);
    }

    private List<CatalogEntry> specialists(ProviderType provider, Collection<String> modules) {
        boolean codex = provider == ProviderType.CODEX;
        List<CatalogEntry> entries = new ArrayList<>();
        for(String module : new TreeSet<>(modules)) {
            for(Technician technician : Technician.values()) {
                String header = PROJECT_AGENTS + "/" + provider.id() + "/" + technician.slug() + (codex ? ".toml" : ".md");
                if(!ClasspathResources.exists(header))
                    continue;
                // Los agentes de Codex se nombran con "_" (analista_core.toml); el resto, con "-"
                String name = technician.slug() + (codex ? "_" : "-") + module;
                entries.add(new CatalogEntry(provider, ArtifactScope.PROJECT, ArtifactKind.AGENT, name,
                    "agents/" + name + (codex ? ".toml" : ".md"), codex ? ArtifactFormat.CODEX_TOML : ArtifactFormat.MARKDOWN,
                    header, PROJECT_AGENTS + SHARED + technician.slug() + "-template.md", technician, module));

                String profile = PROJECT_PROFILES + "/" + technician.slug() + PROFILE_SUFFIX;
                if(codex && ClasspathResources.exists(profile)) {
                    String profileName = technician.slug() + "-" + module;
                    entries.add(new CatalogEntry(provider, ArtifactScope.PROJECT, ArtifactKind.PROFILE, profileName,
                        "profiles/" + profileName + ".toml", null, profile, null, technician, module));
                }
            }
        }
        return entries;
    }

    // El router de Codex en proyecto es una skill: <router>/SKILL.md; en el resto, un agente <router>.md
    private static String routerHeader(ProviderType provider, String name) {
        String base = PROJECT_AGENTS + "/" + provider.id() + "/";
        return provider == ProviderType.CODEX ? base + name + "/SKILL.md" : base + name + ".md";
    }

    private List<CatalogEntry> skills(ProviderType provider, ArtifactScope scope, String skillsDir) throws IOException {
        List<CatalogEntry> entries = new ArrayList<>();
        for(String skill : ClasspathResources.listDirectories(skillsDir).orElse(List.of())) {
            entries.add(new CatalogEntry(provider, scope, ArtifactKind.SKILL, skill, "skills/" + skill, null,
                skillsDir + "/" + skill, null, null, null));
        }
        return entries;
    }

    private static String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }
}
