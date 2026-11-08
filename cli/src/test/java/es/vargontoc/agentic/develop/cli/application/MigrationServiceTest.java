package es.vargontoc.agentic.develop.cli.application;

import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.application.GroupPropagator.Member;
import es.vargontoc.agentic.develop.cli.application.GroupPropagator.Outcome;
import es.vargontoc.agentic.develop.cli.application.GroupPropagator.Result;
import es.vargontoc.agentic.develop.cli.application.MigrationService.Adoption;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactStatus;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.metadata.ContentHash;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupIds;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

/** Artefactos anteriores a los metadatos: personalizados, no propagables, adoptables de forma explícita. */
class MigrationServiceTest {

    private static final String HEADER = "---\nname: x\n---\n\n";
    private static final String GROUP = "global/agent/sdd-agent";

    @TempDir
    Path tmp;

    PathResolver paths;
    Path appHome;
    Path project;
    MetadataStore metadata;
    ArtifactDescriptor descriptor;
    GroupPropagator propagator;
    MigrationService migration;

    @BeforeEach
    void setUp() throws IOException {
        paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        appHome = Files.createDirectories(tmp.resolve("app-home"));
        Path backups = appHome.resolve("backups");
        project = Files.createDirectories(tmp.resolve("proyecto"));

        var catalog = new ResourceArtifactCatalog();
        metadata = new MetadataStore(appHome.resolve("metadata.json"), new TextStore(paths, List.of(appHome), backups));
        var inventory = new ArtifactInventory(paths, catalog, metadata, backups);
        var workspace = new ProjectWorkspace(paths, backups);
        descriptor = new ArtifactDescriptor(paths, catalog, inventory, metadata, workspace, backups);
        var saver = new ArtifactSaver(paths, inventory, metadata, backups);
        migration = new MigrationService(paths, inventory, workspace, metadata, saver);
        propagator = new GroupPropagator(metadata, descriptor, saver, migration);
    }

    private Path agent(ProviderType provider, String content) throws IOException {
        Path file = globalPath(provider, ArtifactKind.AGENT, "sdd-agent", "agents/sdd-agent.md");
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }

    // Ubicación física de un artefacto global según el proveedor (Gemini: agents/<n>/agent.md, skills fuera de config)
    private Path globalPath(ProviderType provider, ArtifactKind kind, String name, String relative) {
        CatalogEntry entry = new CatalogEntry(provider, ArtifactScope.GLOBAL, kind, name, relative, null, null, null, null, null);
        return paths.globalBase(provider).resolve(entry.location());
    }

    private ArtifactLocation at(Path path) {
        return descriptor.resolve(path.toString(), null);
    }

    private static Member of(Result result, Path path) {
        return result.members().stream().filter(m -> m.path().equals(path)).findFirst().orElseThrow();
    }

    @Test
    void groupIdsAreADeterministicFunctionOfTheCatalogEntry() throws IOException {
        Path claude = agent(ProviderType.CLAUDE, HEADER + "a\n");
        Path gemini = agent(ProviderType.GEMINI, HEADER + "a\n");

        assertEquals(GROUP, GroupIds.of(at(claude).entry()));
        assertEquals(GROUP, GroupIds.of(at(gemini).entry()));
    }

    @Test
    void aLegacyArtifactIsDescribedAsValidAndCustomised() throws IOException {
        Path claude = agent(ProviderType.CLAUDE, HEADER + "viejo\n");

        var description = descriptor.describe(claude.toString(), null);

        assertEquals(ArtifactStatus.LEGACY, description.status());
        assertEquals("viejo\n", description.body());
    }

    @Test
    void propagationSkipsLegacyArtifactsAndReportsThem() throws IOException {
        Path claude = agent(ProviderType.CLAUDE, HEADER + "viejo\n");
        Path gemini = agent(ProviderType.GEMINI, HEADER + "viejo\n");
        metadata.putGroup(new GroupRecord(GROUP, ArtifactScope.GLOBAL, "viejo\n"));
        metadata.putArtifact(claude, GROUP, HEADER + "viejo\n", LinkState.LINKED);

        Result result = propagator.propagate(GROUP, "nuevo\n", null, true);

        assertEquals(Outcome.UPDATED, of(result, claude).outcome());
        assertEquals(Outcome.CUSTOM, of(result, gemini).outcome());
        assertEquals(HEADER + "viejo\n", Files.readString(gemini));
        assertEquals(List.of(), of(result, gemini).backups());
    }

    @Test
    void adoptionWithoutConfirmationOnlyShowsTheDiff() throws IOException {
        Path gemini = agent(ProviderType.GEMINI, HEADER + "personalizado\n");
        metadata.putGroup(new GroupRecord(GROUP, ArtifactScope.GLOBAL, "compartido\n"));

        Adoption preview = migration.adopt(at(gemini), GROUP, false);

        assertFalse(preview.applied());
        assertEquals("-personalizado\n+compartido", preview.diff().replace("\n+", "\n+"));
        assertNull(preview.backup());
        assertEquals(HEADER + "personalizado\n", Files.readString(gemini));
        assertTrue(metadata.artifact(gemini).isEmpty());
    }

    @Test
    void confirmedAdoptionUpdatesKeepsACopyAndLinksTheArtifact() throws IOException {
        Path gemini = agent(ProviderType.GEMINI, HEADER + "personalizado\n");
        metadata.putGroup(new GroupRecord(GROUP, ArtifactScope.GLOBAL, "compartido\n"));

        Adoption done = migration.adopt(at(gemini), GROUP, true);

        assertTrue(done.applied());
        assertEquals(HEADER + "compartido\n", Files.readString(gemini));
        assertEquals(HEADER + "personalizado\n", Files.readString(done.backup()));
        var record = metadata.artifact(gemini).orElseThrow();
        assertEquals(GROUP, record.groupId());
        assertEquals(LinkState.LINKED, record.state());
        assertEquals(ContentHash.of(HEADER + "compartido\n"), record.hash());
        assertEquals(ArtifactStatus.LINKED, descriptor.describe(gemini.toString(), null).status());
    }

    @Test
    void anAdoptedArtifactReceivesLaterPropagations() throws IOException {
        Path gemini = agent(ProviderType.GEMINI, HEADER + "personalizado\n");
        metadata.putGroup(new GroupRecord(GROUP, ArtifactScope.GLOBAL, "compartido\n"));
        migration.adopt(at(gemini), GROUP, true);

        Result result = propagator.propagate(GROUP, "tercero\n", null, false);

        assertEquals(Outcome.UPDATED, of(result, gemini).outcome());
        assertEquals(HEADER + "tercero\n", Files.readString(gemini));
    }

    @Test
    void adoptingAnAlreadyTrackedArtifactOrAnUnknownGroupIsAnError() throws IOException {
        Path claude = agent(ProviderType.CLAUDE, HEADER + "a\n");
        metadata.putGroup(new GroupRecord(GROUP, ArtifactScope.GLOBAL, "x\n"));
        metadata.putArtifact(claude, GROUP, HEADER + "a\n", LinkState.LINKED);

        assertEquals(AppErrorCode.ARTIFACT_NOT_LEGACY,
            assertThrows(PathDomainException.class, () -> migration.adopt(at(claude), GROUP, true)).code());
        assertEquals(AppErrorCode.GROUP_NOT_FOUND,
            assertThrows(PathDomainException.class, () -> migration.adopt(at(claude), "otro", true)).code());
        assertEquals(HEADER + "a\n", Files.readString(claude));
    }

    @Test
    void aDetachedArtifactStaysSkippedInLaterPropagations() throws IOException {
        Path claude = agent(ProviderType.CLAUDE, HEADER + "viejo\n");
        metadata.putGroup(new GroupRecord(GROUP, ArtifactScope.GLOBAL, "viejo\n"));
        metadata.putArtifact(claude, GROUP, HEADER + "viejo\n", LinkState.LINKED);

        assertTrue(migration.detach(at(claude)));
        assertFalse(migration.detach(at(claude)));

        for(int i = 0; i < 2; i++)
            assertEquals(Outcome.DETACHED, of(propagator.propagate(GROUP, "nuevo" + i + "\n", null, true), claude).outcome());
        assertEquals(HEADER + "viejo\n", Files.readString(claude));
        assertEquals(ArtifactStatus.DETACHED, descriptor.describe(claude.toString(), null).status());
    }

    @Test
    void detachingALegacyArtifactChangesNothing() throws IOException {
        Path claude = agent(ProviderType.CLAUDE, HEADER + "a\n");

        assertFalse(migration.detach(at(claude)));
        assertTrue(metadata.artifact(claude).isEmpty());
    }

    @Test
    void corruptMetadataNeverDeletesArtifactsAndTheyBecomeLegacy() throws IOException {
        Path claude = agent(ProviderType.CLAUDE, HEADER + "a\n");
        Files.writeString(appHome.resolve("metadata.json"), "{ esto no es json");

        assertEquals(ArtifactStatus.LEGACY, descriptor.describe(claude.toString(), null).status());
        assertEquals(HEADER + "a\n", Files.readString(claude));
        assertEquals(AppErrorCode.GROUP_NOT_FOUND,
            assertThrows(PathDomainException.class, () -> propagator.propagate(GROUP, "x\n", null, true)).code());
        assertEquals(HEADER + "a\n", Files.readString(claude));
    }

    @Test
    void projectLegacySpecialistsOfAnyModuleAreFoundThroughTheirGroup() throws IOException {
        Files.createDirectories(project.resolve(".sdd"));
        Files.writeString(project.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\n- beta | - | -\n");
        Path alfa = project.resolve(".claude/agents/analista-alfa.md");
        Path beta = project.resolve(".claude/agents/analista-beta.md");
        Files.createDirectories(alfa.getParent());
        Files.writeString(alfa, HEADER + "alfa\n");
        Files.writeString(beta, HEADER + "beta\n");
        String group = "project/specialist/analista";
        metadata.putGroup(new GroupRecord(group, ArtifactScope.PROJECT, "Eres de {module}\n"));
        metadata.putArtifact(alfa, group, HEADER + "alfa\n", LinkState.LINKED);

        Result result = propagator.propagate(group, "Eres de {module}\n", project, false);

        assertEquals(Outcome.UPDATED, of(result, alfa).outcome());
        assertEquals(Outcome.CUSTOM, of(result, beta).outcome());
        assertEquals("beta", of(result, beta).module());
    }
}
