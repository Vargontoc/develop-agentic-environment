package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactStatus;
import es.vargontoc.agentic.develop.cli.domain.artifact.InstalledArtifact;
import es.vargontoc.agentic.develop.cli.domain.artifact.Inventory;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

class ArtifactInventoryTest {

    @TempDir
    Path tmp;

    Path claude;
    Path project;
    MetadataStore metadata;
    ArtifactInventory inventory;

    @BeforeEach
    void setUp() throws IOException {
        PathResolver paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        Path appHome = Files.createDirectories(tmp.resolve("app-home"));
        claude = Files.createDirectories(tmp.resolve(".claude"));
        project = Files.createDirectories(tmp.resolve("proyecto"));
        metadata = new MetadataStore(appHome.resolve("metadata.json"),
            new TextStore(paths, List.of(appHome), appHome.resolve("backups")));
        inventory = new ArtifactInventory(paths, new ResourceArtifactCatalog(), metadata, appHome.resolve("backups"));
    }

    private Path agent(Path root, String name, String content) throws IOException {
        Path file = root.resolve("agents").resolve(name);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }

    private Path skill(Path root, String name, String skillMd) throws IOException {
        Path dir = Files.createDirectories(root.resolve("skills").resolve(name));
        Files.writeString(dir.resolve("SKILL.md"), skillMd);
        return dir;
    }

    private static List<String> relativePaths(Inventory inv) {
        return inv.artifacts().stream().map(a -> a.entry().relativePath()).toList();
    }

    @Test
    void onlyCataloguedArtifactsAreListedAndForeignOnesAreExcluded() throws IOException {
        agent(claude, "sdd-agent.md", "uno");
        agent(claude, "mi-agente-propio.md", "mio");
        skill(claude, "sdd-design", "skill");
        skill(claude, "mi-skill-propia", "mia");

        Inventory inv = inventory.scanGlobal(ProviderType.CLAUDE);

        assertEquals(List.of("agents/sdd-agent.md", "skills/sdd-design"), relativePaths(inv));
        assertTrue(inv.problems().isEmpty());
    }

    @Test
    void jarResourcesThatAreNotInstalledDoNotAppear() {
        assertTrue(inventory.scanGlobal(ProviderType.CLAUDE).artifacts().isEmpty());
        assertTrue(inventory.scanGlobal(ProviderType.GEMINI).artifacts().isEmpty());
    }

    @Test
    void severalProvidersAreInventoriedIndependentlyWithStableHashes() throws IOException {
        Path codex = Files.createDirectories(tmp.resolve(".codex"));
        agent(claude, "sdd-agent.md", "claude");
        agent(codex, "sdd_agent.toml", "codex");

        Inventory first = inventory.scanGlobal(ProviderType.CLAUDE);
        Inventory again = inventory.scanGlobal(ProviderType.CLAUDE);
        Inventory codexInv = inventory.scanGlobal(ProviderType.CODEX);

        assertEquals(first, again);
        assertEquals(List.of("agents/sdd_agent.toml"), relativePaths(codexInv));
        assertFalse(first.artifacts().get(0).hash().equals(codexInv.artifacts().get(0).hash()));
    }

    @Test
    void artifactsWithoutMetadataAreLegacy() throws IOException {
        agent(claude, "sdd-agent.md", "uno");

        InstalledArtifact artifact = inventory.scanGlobal(ProviderType.CLAUDE).artifacts().get(0);

        assertEquals(ArtifactStatus.LEGACY, artifact.status());
        assertFalse(artifact.modifiedExternally());
    }

    @Test
    void registeredArtifactsAreLinkedDetachedOrModified() throws IOException {
        Path agent = agent(claude, "sdd-agent.md", "uno");
        Path other = agent(claude, "sdd-design.md", "dos");
        metadata.putArtifact(agent, "g", "uno", LinkState.LINKED);
        metadata.putArtifact(other, "g", "dos", LinkState.LINKED);
        metadata.markDetached(other);
        Files.writeString(agent, "uno editado fuera");

        List<InstalledArtifact> artifacts = inventory.scanGlobal(ProviderType.CLAUDE).artifacts();

        InstalledArtifact modified = artifacts.stream().filter(a -> a.entry().name().equals("sdd-agent")).findFirst().orElseThrow();
        InstalledArtifact detached = artifacts.stream().filter(a -> a.entry().name().equals("sdd-design")).findFirst().orElseThrow();
        assertEquals(ArtifactStatus.LINKED, modified.status());
        assertTrue(modified.modifiedExternally());
        assertEquals(ArtifactStatus.DETACHED, detached.status());
        assertFalse(detached.modifiedExternally());
    }

    @Test
    void lineEndingChangesAreNotExternalModifications() throws IOException {
        Path agent = agent(claude, "sdd-agent.md", "a\nb\n");
        metadata.putArtifact(agent, "g", "a\nb\n", LinkState.LINKED);
        Files.writeString(agent, "a\r\nb\r\n");

        assertFalse(inventory.scanGlobal(ProviderType.CLAUDE).artifacts().get(0).modifiedExternally());
    }

    @Test
    void skillHashCoversEveryFileIncludingBinaryAssets() throws IOException {
        Path dir = skill(claude, "sdd-design", "skill");
        String base = inventory.scanGlobal(ProviderType.CLAUDE).artifacts().get(0).hash();

        Files.write(dir.resolve("logo.bin"), new byte[] {(byte) 0xC3, (byte) 0x28, 0x00});
        String withAsset = inventory.scanGlobal(ProviderType.CLAUDE).artifacts().get(0).hash();
        Files.write(dir.resolve("logo.bin"), new byte[] {(byte) 0xC3, (byte) 0x29, 0x00});
        String changedAsset = inventory.scanGlobal(ProviderType.CLAUDE).artifacts().get(0).hash();
        Files.delete(dir.resolve("logo.bin"));
        String removed = inventory.scanGlobal(ProviderType.CLAUDE).artifacts().get(0).hash();

        assertFalse(base.equals(withAsset));
        assertFalse(withAsset.equals(changedAsset));
        assertEquals(base, removed);
    }

    @Test
    void unreadableArtifactIsReportedWithItsPathAndTheRestIsStillListed() throws IOException {
        Path broken = agent(claude, "sdd-agent.md", "x");
        Files.write(broken, new byte[] {(byte) 0xC3, (byte) 0x28});
        agent(claude, "sdd-design.md", "bien");

        Inventory inv = inventory.scanGlobal(ProviderType.CLAUDE);

        assertEquals(List.of("agents/sdd-design.md"), relativePaths(inv));
        assertEquals(1, inv.problems().size());
        assertEquals(broken, inv.problems().get(0).path());
        assertEquals(AppErrorCode.TEXT_READ_FAILED, inv.problems().get(0).code());
    }

    @Test
    void anArtifactOfTheWrongKindIsAProblem() throws IOException {
        Files.createDirectories(claude.resolve("skills"));
        Path skillAsFile = Files.writeString(claude.resolve("skills").resolve("sdd-design"), "no soy un directorio");

        Inventory inv = inventory.scanGlobal(ProviderType.CLAUDE);

        assertTrue(inv.artifacts().isEmpty());
        assertEquals(skillAsFile, inv.problems().get(0).path());
        assertEquals(AppErrorCode.ARTIFACT_KIND_MISMATCH, inv.problems().get(0).code());
    }

    @Test
    void projectScopeListsRoutersAndSpecialistsOfRegisteredModulesOnly() throws IOException {
        Path providerDir = Files.createDirectories(project.resolve(".claude"));
        agent(providerDir, "analista-router.md", "router");
        agent(providerDir, "analista-alfa.md", "especialista");
        agent(providerDir, "analista-propio.md", "de otro");
        skill(providerDir, "team-sprints", "skill");

        Inventory inv = inventory.scanProject(project, ProviderType.CLAUDE, List.of("alfa"));

        assertEquals(List.of("skills/team-sprints", "agents/analista-router.md", "agents/analista-alfa.md"), relativePaths(inv));
    }

    @Test
    void projectScopeIsEmptyForProvidersWithoutProjectSupport() {
        assertTrue(inventory.scanProject(project, ProviderType.CODEX, List.of("alfa")).artifacts().isEmpty());
    }

    @Test
    void providerFolderThatDoesNotExistGivesAnEmptyInventory() {
        Inventory inv = inventory.scanGlobal(ProviderType.OPENCODE);

        assertTrue(inv.artifacts().isEmpty());
        assertTrue(inv.problems().isEmpty());
    }
}
