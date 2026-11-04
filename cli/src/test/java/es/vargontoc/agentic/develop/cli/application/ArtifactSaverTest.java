package es.vargontoc.agentic.develop.cli.application;

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

import es.vargontoc.agentic.develop.cli.application.ArtifactSaver.SaveResult;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.metadata.ContentHash;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

class ArtifactSaverTest {

    private static final String HEADER = "---\nname: sdd-agent\n---\n\n";
    private static final String ORIGINAL = HEADER + "Eres el agente.\nSegunda línea.\n";

    @TempDir
    Path tmp;

    Path agent;
    Path backups;
    MetadataStore metadata;
    ArtifactDescriptor descriptor;
    ArtifactSaver saver;

    @BeforeEach
    void setUp() throws IOException {
        PathResolver paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        Path appHome = Files.createDirectories(tmp.resolve("app-home"));
        backups = appHome.resolve("backups");
        agent = tmp.resolve(".claude/agents/sdd-agent.md");
        Files.createDirectories(agent.getParent());
        Files.writeString(agent, ORIGINAL);

        var catalog = new ResourceArtifactCatalog();
        metadata = new MetadataStore(appHome.resolve("metadata.json"), new TextStore(paths, List.of(appHome), backups));
        var inventory = new ArtifactInventory(paths, catalog, metadata, backups);
        descriptor = new ArtifactDescriptor(paths, catalog, inventory, metadata, new ProjectWorkspace(paths, backups), backups);
        saver = new ArtifactSaver(paths, inventory, metadata, backups);
    }

    private SaveResult save(String body, boolean overwrite) {
        return saver.saveInstance(descriptor.resolve(agent.toString(), null), body, overwrite);
    }

    private void track() {
        metadata.putArtifact(agent, "global/sdd-agent", ORIGINAL, LinkState.LINKED);
    }

    @Test
    void savingAnInstanceUpdatesContentHashAndBackup() throws IOException {
        track();

        SaveResult result = save("Eres otro agente.\nSegunda línea.\n", false);

        assertTrue(result.written());
        assertTrue(result.tracked());
        String saved = HEADER + "Eres otro agente.\nSegunda línea.\n";
        assertEquals(saved, Files.readString(agent));
        assertEquals(ContentHash.of(saved), metadata.artifact(agent).orElseThrow().hash());
        assertEquals(LinkState.LINKED, metadata.artifact(agent).orElseThrow().state());
        assertEquals(ORIGINAL, Files.readString(result.backup()));
        assertTrue(result.backup().startsWith(backups));
        assertEquals("-Eres el agente.\n+Eres otro agente.".replace("\n+", "\n+"), result.diff());
    }

    @Test
    void savingWithoutDifferencesWritesNothing() throws IOException {
        track();
        long before = Files.getLastModifiedTime(agent).toMillis();

        SaveResult result = save("Eres el agente.\nSegunda línea.\n", false);

        assertFalse(result.written());
        assertNull(result.backup());
        assertEquals("", result.diff());
        assertEquals(ORIGINAL, Files.readString(agent));
        assertEquals(before, Files.getLastModifiedTime(agent).toMillis());
        assertFalse(Files.exists(backups));
    }

    @Test
    void anExternalChangeIsAConflictAndNothingIsWritten() throws IOException {
        track();
        String external = HEADER + "Editado por otra herramienta.\n";
        Files.writeString(agent, external);

        PathDomainException e = assertThrows(PathDomainException.class, () -> save("Mi cambio.\n", false));

        assertEquals(AppErrorCode.ARTIFACT_CONFLICT, e.code());
        assertEquals(agent.toString(), e.location());
        assertEquals(external, Files.readString(agent));
        assertFalse(Files.exists(backups));
    }

    @Test
    void explicitOverwriteWritesAndKeepsACopyOfTheExternalVersion() throws IOException {
        track();
        String external = HEADER + "Editado por otra herramienta.\n";
        Files.writeString(agent, external);

        SaveResult result = save("Mi cambio.\n", true);

        assertTrue(result.written());
        assertEquals(HEADER + "Mi cambio.\n", Files.readString(agent));
        assertEquals(external, Files.readString(result.backup()));
        assertFalse(metadata.artifact(agent).isEmpty());
        assertEquals(ContentHash.of(HEADER + "Mi cambio.\n"), metadata.artifact(agent).orElseThrow().hash());
    }

    @Test
    void overwriteWithTheSameContentRealignsTheRecordWithoutWriting() throws IOException {
        track();
        String external = HEADER + "Igual.\n";
        Files.writeString(agent, external);

        SaveResult result = save("Igual.\n", true);

        assertFalse(result.written());
        assertEquals(ContentHash.of(external), metadata.artifact(agent).orElseThrow().hash());
    }

    @Test
    void aLegacyArtifactIsSavedWithoutCreatingARecord() throws IOException {
        SaveResult result = save("Nuevo.\n", false);

        assertTrue(result.written());
        assertFalse(result.tracked());
        assertTrue(metadata.artifact(agent).isEmpty());
        assertEquals(HEADER + "Nuevo.\n", Files.readString(agent));
    }

    @Test
    void crlfIsPreserved() throws IOException {
        Files.writeString(agent, ORIGINAL.replace("\n", "\r\n"));

        save("Eres otro agente.\n", false);

        assertEquals((HEADER + "Eres otro agente.\n").replace("\n", "\r\n"), Files.readString(agent));
    }

    @Test
    void theHeaderIsNeverTouched() throws IOException {
        Files.writeString(agent, "---\nname: sdd-agent\n# nota que debe sobrevivir\n---\n\ncuerpo\n");

        save("otro\n", false);

        assertEquals("---\nname: sdd-agent\n# nota que debe sobrevivir\n---\n\notro\n", Files.readString(agent));
    }

    @Test
    void aFailedWriteLeavesTheOriginalIntact() throws IOException {
        track();
        Files.writeString(agent, ORIGINAL);
        // Una cabecera no reconocible hace fallar antes de escribir
        Path broken = tmp.resolve(".claude/agents/sdd-agent.md");
        Files.writeString(broken, "sin cabecera\n");
        metadata.putArtifact(broken, "global/sdd-agent", "sin cabecera\n", LinkState.LINKED);

        PathDomainException e = assertThrows(PathDomainException.class, () -> save("x\n", false));

        assertEquals(AppErrorCode.ARTIFACT_FORMAT_INVALID, e.code());
        assertEquals("sin cabecera\n", Files.readString(broken));
    }
}
