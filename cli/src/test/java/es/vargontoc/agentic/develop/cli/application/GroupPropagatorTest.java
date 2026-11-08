package es.vargontoc.agentic.develop.cli.application;

import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.metadata.ContentHash;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

class GroupPropagatorTest {

    private static final String HEADER = "---\nname: x\n---\n\n";

    @TempDir
    Path tmp;

    PathResolver paths;
    Path project;
    MetadataStore metadata;
    GroupPropagator propagator;

    @BeforeEach
    void setUp() throws IOException {
        paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        Path appHome = Files.createDirectories(tmp.resolve("app-home"));
        Path backups = appHome.resolve("backups");
        project = Files.createDirectories(tmp.resolve("proyecto"));
        Files.createDirectories(project.resolve(".sdd"));
        Files.writeString(project.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\n- beta | - | -\n");

        var catalog = new ResourceArtifactCatalog();
        metadata = new MetadataStore(appHome.resolve("metadata.json"), new TextStore(paths, List.of(appHome), backups));
        var inventory = new ArtifactInventory(paths, catalog, metadata, backups);
        var descriptor = new ArtifactDescriptor(paths, catalog, inventory, metadata, new ProjectWorkspace(paths, backups), backups);
        var workspace = new ProjectWorkspace(paths, backups);
        var saver = new ArtifactSaver(paths, inventory, metadata, backups);
        propagator = new GroupPropagator(metadata, descriptor, saver, new MigrationService(paths, inventory, workspace, metadata, saver));
    }

    private Path file(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content);
    }

    private Path globalAgent(ProviderType provider, String content) throws IOException {
        return file(globalPath(provider, ArtifactKind.AGENT, "sdd-agent", "agents/sdd-agent.md"), content);
    }

    // Ubicación física de un artefacto global según el proveedor (Gemini: agents/<n>/agent.md, skills fuera de config)
    private Path globalPath(ProviderType provider, ArtifactKind kind, String name, String relative) {
        CatalogEntry entry = new CatalogEntry(provider, ArtifactScope.GLOBAL, kind, name, relative, null, null, null, null, null);
        return paths.globalBase(provider).resolve(entry.location());
    }

    private Path specialist(String module, String content) throws IOException {
        return file(project.resolve(".claude/agents/analista-" + module + ".md"), content);
    }

    private void group(String id, ArtifactScope scope, String body, Path... members) {
        metadata.putGroup(new GroupRecord(id, scope, body));
        for(Path member : members)
            track(id, member, LinkState.LINKED);
    }

    private void track(String id, Path path, LinkState state) {
        try {
            metadata.putArtifact(path, id, Files.readString(path), state);
        } catch(IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Member of(Result result, Path path) {
        return result.members().stream().filter(m -> m.path().equals(path)).findFirst().orElseThrow();
    }

    @Test
    void aSharedSpecialistBodyUpdatesEveryModuleKeepingItsOwnName() throws IOException {
        Path alfa = specialist("alfa", HEADER + "Trabajas en alfa.\n");
        Path beta = specialist("beta", HEADER + "Trabajas en beta.\n");
        group("analista", ArtifactScope.PROJECT, "Trabajas en {module}.\n", alfa, beta);

        Result result = propagator.propagate("analista", "Eres analista de {module}.\n", project, false);

        assertTrue(result.changed());
        assertEquals(HEADER + "Eres analista de alfa.\n", Files.readString(alfa));
        assertEquals(HEADER + "Eres analista de beta.\n", Files.readString(beta));
        assertEquals(Outcome.UPDATED, of(result, alfa).outcome());
        assertEquals("alfa", of(result, alfa).module());
        assertEquals("-Trabajas en alfa.\n+Eres analista de alfa.".replace("\n+", "\n+"), of(result, alfa).diff());
        assertTrue(Files.exists(of(result, beta).backups().get(0)));
        assertEquals("Eres analista de {module}.\n", metadata.group("analista").orElseThrow().sharedBody());
        assertEquals(ContentHash.of(HEADER + "Eres analista de beta.\n"), metadata.artifact(beta).orElseThrow().hash());
    }

    @Test
    void aGlobalAgentUpdatesEveryInstalledProvider() throws IOException {
        Path claude = globalAgent(ProviderType.CLAUDE, HEADER + "viejo\n");
        Path gemini = globalAgent(ProviderType.GEMINI, HEADER + "viejo\n");
        group("sdd-agent", ArtifactScope.GLOBAL, "viejo\n", claude, gemini);

        Result result = propagator.propagate("sdd-agent", "nuevo\n", null, false);

        assertEquals(List.of(Outcome.UPDATED, Outcome.UPDATED), result.members().stream().map(Member::outcome).toList());
        assertEquals(HEADER + "nuevo\n", Files.readString(claude));
        assertEquals(HEADER + "nuevo\n", Files.readString(gemini));
    }

    @Test
    void aProviderWithoutTheArtifactIsSkippedAndNeverCreated() throws IOException {
        Path claude = globalAgent(ProviderType.CLAUDE, HEADER + "viejo\n");
        Path gemini = globalAgent(ProviderType.GEMINI, HEADER + "viejo\n");
        group("sdd-agent", ArtifactScope.GLOBAL, "viejo\n", claude, gemini);
        Files.delete(gemini);

        Result result = propagator.propagate("sdd-agent", "nuevo\n", null, false);

        assertEquals(Outcome.MISSING, of(result, gemini).outcome());
        assertEquals(Outcome.UPDATED, of(result, claude).outcome());
        assertFalse(Files.exists(gemini));
    }

    @Test
    void anUnchangedGroupWritesNothing() throws IOException {
        Path claude = globalAgent(ProviderType.CLAUDE, HEADER + "igual\n");
        group("sdd-agent", ArtifactScope.GLOBAL, "igual\n", claude);

        Result result = propagator.propagate("sdd-agent", "igual\n", null, false);

        assertFalse(result.changed());
        assertFalse(result.groupUpdated());
        assertEquals(Outcome.UNCHANGED, of(result, claude).outcome());
        assertEquals(HEADER + "igual\n", Files.readString(claude));
        // Las copias que haya son del almacén de metadatos al prepararse la prueba, no del agente
        try(var backups = Files.list(tmp.resolve("app-home/backups"))) {
            assertTrue(backups.noneMatch(b -> b.getFileName().toString().endsWith("sdd-agent.md.bak")));
        }
    }

    @Test
    void membersOfAnotherScopeAreNotPropagated() throws IOException {
        Path global = globalAgent(ProviderType.CLAUDE, HEADER + "viejo\n");
        Path alfa = specialist("alfa", HEADER + "viejo alfa\n");
        group("sdd-agent", ArtifactScope.GLOBAL, "viejo\n", global, alfa);

        Result result = propagator.propagate("sdd-agent", "nuevo\n", project, false);

        assertEquals(Outcome.OUT_OF_SCOPE, of(result, alfa).outcome());
        assertEquals(HEADER + "viejo alfa\n", Files.readString(alfa));
        assertEquals(Outcome.UPDATED, of(result, global).outcome());
    }

    @Test
    void projectMembersOfAnotherProjectAreNotTouched() throws IOException {
        Path alfa = specialist("alfa", HEADER + "viejo\n");
        group("analista", ArtifactScope.PROJECT, "viejo\n", alfa);
        Path otherProject = Files.createDirectories(tmp.resolve("otro"));
        Files.createDirectories(otherProject.resolve(".sdd"));
        Files.writeString(otherProject.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\n");

        Result result = propagator.propagate("analista", "nuevo\n", otherProject, false);

        assertEquals(Outcome.OUT_OF_SCOPE, of(result, alfa).outcome());
        assertEquals(HEADER + "viejo\n", Files.readString(alfa));
    }

    @Test
    void conflictingMembersAreSkippedUnlessOverwriteIsRequested() throws IOException {
        Path claude = globalAgent(ProviderType.CLAUDE, HEADER + "viejo\n");
        Path gemini = globalAgent(ProviderType.GEMINI, HEADER + "viejo\n");
        group("sdd-agent", ArtifactScope.GLOBAL, "viejo\n", claude, gemini);
        Files.writeString(gemini, HEADER + "editado fuera\n");

        Result skipped = propagator.propagate("sdd-agent", "nuevo\n", null, false);

        assertEquals(Outcome.CONFLICT, of(skipped, gemini).outcome());
        assertEquals(HEADER + "editado fuera\n", Files.readString(gemini));
        assertEquals(HEADER + "nuevo\n", Files.readString(claude));

        Result forced = propagator.propagate("sdd-agent", "nuevo\n", null, true);

        assertEquals(Outcome.UPDATED, of(forced, gemini).outcome());
        assertEquals(HEADER + "nuevo\n", Files.readString(gemini));
        assertEquals(HEADER + "editado fuera\n", Files.readString(of(forced, gemini).backups().get(0)));
    }

    @Test
    void detachedMembersAreNeverPropagated() throws IOException {
        Path claude = globalAgent(ProviderType.CLAUDE, HEADER + "viejo\n");
        group("sdd-agent", ArtifactScope.GLOBAL, "viejo\n");
        track("sdd-agent", claude, LinkState.DETACHED);

        Result result = propagator.propagate("sdd-agent", "nuevo\n", null, true);

        assertEquals(Outcome.DETACHED, of(result, claude).outcome());
        assertEquals(HEADER + "viejo\n", Files.readString(claude));
    }

    @Test
    void anUnknownGroupIsAnError() {
        PathDomainException e = assertThrows(PathDomainException.class, () -> propagator.propagate("nada", "x", null, false));

        assertEquals(AppErrorCode.GROUP_NOT_FOUND, e.code());
        assertEquals("nada", e.location());
    }
}
