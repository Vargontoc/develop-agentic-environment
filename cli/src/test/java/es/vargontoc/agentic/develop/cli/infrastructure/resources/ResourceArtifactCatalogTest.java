package es.vargontoc.agentic.develop.cli.infrastructure.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;

class ResourceArtifactCatalogTest {

    private final ResourceArtifactCatalog catalog = new ResourceArtifactCatalog();

    @Test
    void globalAgentIsIdentifiedWithProviderKindAndFormat() {
        CatalogEntry entry = catalog.identify(ProviderType.CLAUDE, ArtifactScope.GLOBAL, "agents/sdd-agent.md", List.of()).orElseThrow();

        assertEquals(ProviderType.CLAUDE, entry.provider());
        assertEquals(ArtifactKind.AGENT, entry.kind());
        assertEquals(ArtifactFormat.MARKDOWN, entry.format());
        assertEquals("sdd-agent", entry.name());
        assertFalse(entry.isDirectory());
    }

    @Test
    void codexAgentsAreTomlAndTheProfileIsPartOfTheCatalog() {
        CatalogEntry agent = catalog.identify(ProviderType.CODEX, ArtifactScope.GLOBAL, "agents/sdd_agent.toml", List.of()).orElseThrow();
        CatalogEntry profile = catalog.identify(ProviderType.CODEX, ArtifactScope.GLOBAL, "profiles/sdd.config.toml", List.of()).orElseThrow();

        assertEquals(ArtifactFormat.CODEX_TOML, agent.format());
        assertEquals(ArtifactKind.PROFILE, profile.kind());
    }

    @Test
    void installedSkillIsRecognizedAsADirectoryEvenWithBackslashes() {
        CatalogEntry skill = catalog.identify(ProviderType.GEMINI, ArtifactScope.GLOBAL, "skills\\sdd-design", List.of()).orElseThrow();

        assertEquals(ArtifactKind.SKILL, skill.kind());
        assertTrue(skill.isDirectory());
    }

    @Test
    void userOwnedAgentsAndJarResourcePathsAreNotCatalogued() {
        assertEquals(Optional.empty(),
            catalog.identify(ProviderType.CLAUDE, ArtifactScope.GLOBAL, "agents/mi-agente-propio.md", List.of()));
        assertEquals(Optional.empty(),
            catalog.identify(ProviderType.CLAUDE, ArtifactScope.GLOBAL, "/global/agents/claude/sdd-agent.md", List.of()));
    }

    @Test
    void unknownProviderIsAnError() {
        DomainException e = assertThrows(DomainException.class, () -> ProviderType.fromId("cursor"));

        assertEquals(AppErrorCode.PROVIDER_UNKNOWN, e.code());
        assertEquals(ProviderType.OPENCODE, ProviderType.fromId("opencode"));
    }

    @Test
    void everyGlobalProviderHasAgentsAndSkills() {
        for(ProviderType provider : ProviderType.values()) {
            List<CatalogEntry> entries = catalog.entries(provider, ArtifactScope.GLOBAL);

            assertTrue(entries.stream().anyMatch(e -> e.kind() == ArtifactKind.AGENT), provider + " sin agentes");
            assertTrue(entries.stream().anyMatch(e -> e.kind() == ArtifactKind.SKILL), provider + " sin skills");
        }
    }

    @Test
    void projectScopeHasSharedRoutersAndSpecialistsOnlyForRegisteredModules() {
        List<String> withoutModules = names(catalog.entries(ProviderType.CLAUDE, ArtifactScope.PROJECT));
        List<String> withModules = names(catalog.entries(ProviderType.CLAUDE, ArtifactScope.PROJECT, List.of("zeta", "alfa")));

        assertTrue(withoutModules.contains("analista-router"));
        assertFalse(withoutModules.contains("analista-alfa"));
        assertTrue(withModules.contains("analista-alfa"));
        assertTrue(withModules.contains("desarrollador-zeta"));
        assertEquals(Optional.empty(),
            catalog.identify(ProviderType.CLAUDE, ArtifactScope.PROJECT, "agents/analista-otro.md", List.of("alfa")));
    }

    @Test
    void codexProjectHasSkillRoutersSpecialistsAndOneProfilePerAgentAndModule() {
        List<CatalogEntry> entries = catalog.entries(ProviderType.CODEX, ArtifactScope.PROJECT, List.of("alfa"));

        CatalogEntry router = entries.stream().filter(e -> e.name().equals("analista-router")).findFirst().orElseThrow();
        assertEquals(".agents/skills/analista-router/SKILL.md", router.location());
        CatalogEntry specialist = entries.stream().filter(e -> e.name().equals("analista_alfa")).findFirst().orElseThrow();
        assertEquals(".codex/agents/analista_alfa.toml", specialist.location());
        assertEquals(ArtifactFormat.CODEX_TOML, specialist.format());
        CatalogEntry profile = entries.stream().filter(e -> e.kind() == ArtifactKind.PROFILE && e.module() != null
            && e.name().equals("desarrollador-alfa")).findFirst().orElseThrow();
        assertEquals(".codex/profiles/desarrollador-alfa.toml", profile.location());
        assertEquals(".agents/skills/team-conventions", entries.stream().filter(e -> e.name().equals("team-conventions"))
            .findFirst().orElseThrow().location());
    }

    @Test
    void geminiHasOneFolderPerAgentAndSharesProjectSkillsWithCodex() {
        List<CatalogEntry> gemini = catalog.entries(ProviderType.GEMINI, ArtifactScope.PROJECT, List.of("alfa"));
        List<CatalogEntry> codex = catalog.entries(ProviderType.CODEX, ArtifactScope.PROJECT, List.of("alfa"));

        assertEquals(".agents/agents/analista-alfa/agent.md", gemini.stream().filter(e -> e.name().equals("analista-alfa"))
            .findFirst().orElseThrow().location());
        assertEquals(".gemini/config/agents/sdd-agent/agent.md", catalog.entries(ProviderType.GEMINI, ArtifactScope.GLOBAL).stream()
            .filter(e -> e.name().equals("sdd-agent")).findFirst().orElseThrow().location());
        assertEquals(".gemini/skills/sdd-design", catalog.entries(ProviderType.GEMINI, ArtifactScope.GLOBAL).stream()
            .filter(e -> e.kind() == ArtifactKind.SKILL && e.name().equals("sdd-design")).findFirst().orElseThrow().location());
        // La misma ruta física en los dos proveedores: .agents/skills/<skill>
        assertEquals(locations(gemini, ArtifactKind.SKILL), locations(codex, ArtifactKind.SKILL));
    }

    private static List<String> locations(List<CatalogEntry> entries, ArtifactKind kind) {
        return entries.stream().filter(e -> e.kind() == kind).map(CatalogEntry::location).sorted().toList();
    }

    @Test
    void catalogOrderIsDeterministic() {
        assertEquals(catalog.entries(ProviderType.CLAUDE, ArtifactScope.GLOBAL),
            new ResourceArtifactCatalog().entries(ProviderType.CLAUDE, ArtifactScope.GLOBAL));
    }

    @Test
    void everyCataloguedResourceExistsInTheJar() {
        for(ProviderType provider : ProviderType.values()) {
            for(ArtifactScope scope : ArtifactScope.values()) {
                for(CatalogEntry entry : catalog.entries(provider, scope, List.of("alfa"))) {
                    assertTrue(ClasspathResources.exists(entry.resource()), "Falta " + entry.resource());
                    if(entry.bodyResource() != null)
                        assertTrue(ClasspathResources.exists(entry.bodyResource()), "Falta " + entry.bodyResource());
                }
            }
        }
    }

    private static List<String> names(List<CatalogEntry> entries) {
        return entries.stream().map(CatalogEntry::name).toList();
    }
}
