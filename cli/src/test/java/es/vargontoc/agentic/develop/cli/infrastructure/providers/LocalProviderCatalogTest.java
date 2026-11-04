package es.vargontoc.agentic.develop.cli.infrastructure.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.ProviderState;
import es.vargontoc.agentic.develop.cli.domain.ProviderStatus;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.CatalogEntry;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

class LocalProviderCatalogTest {

    @TempDir
    Path home;

    ResourceArtifactCatalog artifacts = new ResourceArtifactCatalog();
    LocalProviderCatalog catalog;
    Path claude;

    @BeforeEach
    void setUp() {
        PathResolver paths = new PathResolver(Map.of("HOME", home.toString()), PathResolver.Os.LINUX);
        catalog = new LocalProviderCatalog(paths, artifacts);
        claude = home.resolve(".claude");
    }

    private void install(CatalogEntry entry) throws IOException {
        Path target = claude.resolve(entry.relativePath());
        if(entry.isDirectory()) {
            Files.createDirectories(target);
        } else {
            Files.createDirectories(target.getParent());
            Files.writeString(target, "x");
        }
    }

    private List<CatalogEntry> expected() {
        return artifacts.entries(ProviderType.CLAUDE, ArtifactScope.GLOBAL);
    }

    @Test
    void providerWithoutFolderIsNotInstalled() {
        ProviderStatus status = catalog.status(ProviderType.CLAUDE);

        assertEquals(ProviderState.NOT_INSTALLED, status.state());
        assertTrue(status.missing().isEmpty());
    }

    @Test
    void folderWithOnlyUserArtifactsIsInstalledButUnconfigured() throws IOException {
        Files.createDirectories(claude.resolve("agents"));
        Files.writeString(claude.resolve("agents").resolve("mi-agente.md"), "propio");

        ProviderStatus status = catalog.status(ProviderType.CLAUDE);

        assertEquals(ProviderState.UNCONFIGURED, status.state());
        assertEquals(expected().stream().map(CatalogEntry::relativePath).toList(), status.missing());
    }

    @Test
    void providerWithEveryExpectedArtifactIsConfigured() throws IOException {
        for(CatalogEntry entry : expected())
            install(entry);

        ProviderStatus status = catalog.status(ProviderType.CLAUDE);

        assertEquals(ProviderState.CONFIGURED, status.state());
        assertTrue(status.missing().isEmpty());
    }

    @Test
    void providerWithSomeArtifactsIsPartialAndListsTheMissingOnes() throws IOException {
        List<CatalogEntry> entries = expected();
        for(CatalogEntry entry : entries.subList(1, entries.size()))
            install(entry);

        ProviderStatus status = catalog.status(ProviderType.CLAUDE);

        assertEquals(ProviderState.PARTIAL, status.state());
        assertEquals(List.of(entries.get(0).relativePath()), status.missing());
    }

    @Test
    void availableProvidersOnlyListsExistingFolders() throws IOException {
        Files.createDirectories(claude);

        assertEquals(List.of(ProviderType.CLAUDE), catalog.availableProviders().stream().map(p -> p.getType()).toList());
    }
}
