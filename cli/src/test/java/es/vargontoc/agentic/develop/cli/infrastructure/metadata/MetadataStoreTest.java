package es.vargontoc.agentic.develop.cli.infrastructure.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.metadata.ArtifactRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.ContentHash;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupIds;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

class MetadataStoreTest {

    @TempDir
    Path tmp;

    Path appHome;
    Path project;
    Path file;

    @BeforeEach
    void setUp() throws IOException {
        appHome = Files.createDirectories(tmp.resolve("app-home"));
        project = Files.createDirectories(tmp.resolve("project"));
        file = appHome.resolve("metadata.json");
    }

    private MetadataStore newStore() {
        PathResolver paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        return new MetadataStore(file, new TextStore(paths, List.of(appHome), appHome.resolve("backups")));
    }

    @Test
    void groupWithSeveralArtifactsIsCreatedReadAndUpdated() {
        MetadataStore store = newStore();
        Path a = project.resolve(".claude/agents/analista-uno.md");
        Path b = project.resolve(".claude/agents/analista-dos.md");

        store.putGroup(new GroupRecord("project:analista", ArtifactScope.PROJECT, "Eres analista-{module}"));
        store.putArtifact(a, "project:analista", "contenido uno", LinkState.LINKED);
        store.putArtifact(b, "project:analista", "contenido dos", LinkState.LINKED);
        store.putArtifact(a, "project:analista", "contenido uno v2", LinkState.LINKED);

        MetadataStore reopened = newStore();
        assertEquals("Eres analista-{module}", reopened.group("project:analista").orElseThrow().sharedBody());
        assertEquals(2, reopened.artifactsOfGroup("project:analista").size());
        assertEquals(ContentHash.of("contenido uno v2"), reopened.artifact(a).orElseThrow().hash());
        assertEquals(ContentHash.of("contenido dos"), reopened.artifact(b).orElseThrow().hash());
    }

    @Test
    void schemaVersionIsRecordedInTheFile() throws IOException {
        newStore().putGroup(new GroupRecord("global:sdd-agent", ArtifactScope.GLOBAL, "cuerpo"));

        assertTrue(Files.readString(file).contains("\"schemaVersion\" : 1"));
    }

    @Test
    void markDetachedAndRemoveArtifactArePersisted() {
        MetadataStore store = newStore();
        Path a = project.resolve("a.md");
        store.putArtifact(a, "g", "x", LinkState.LINKED);

        store.markDetached(a);
        assertEquals(LinkState.DETACHED, newStore().artifact(a).orElseThrow().state());

        store.removeArtifact(a);
        assertTrue(newStore().artifact(a).isEmpty());
    }

    @Test
    void absentStoreIsEmpty() {
        MetadataStore store = newStore();

        assertTrue(store.snapshot().groups().isEmpty());
        assertTrue(store.snapshot().artifacts().isEmpty());
        assertEquals(MetadataStore.SCHEMA_VERSION, store.snapshot().schemaVersion());
    }

    @Test
    void corruptUnsupportedOrInconsistentStoresAreTreatedAsEmpty() throws IOException {
        for(String content : List.of(
                "{esto no es json",
                "{\"schemaVersion\":99,\"groups\":{},\"artifacts\":{}}",
                "{\"groups\":{},\"artifacts\":{}}",
                "{\"schemaVersion\":1,\"groups\":{},\"artifacts\":{\"x\":{\"path\":\"otro\",\"groupId\":\"g\",\"hash\":\"h\",\"state\":\"LINKED\"}}}",
                "{\"schemaVersion\":1,\"campoDesconocido\":true}")) {
            Files.writeString(file, content);

            MetadataStore store = newStore();

            assertTrue(store.snapshot().artifacts().isEmpty(), content);
            assertTrue(store.artifact(project.resolve("x")).isEmpty(), content);
        }
    }

    @Test
    void writingOverACorruptStoreKeepsABackupOfIt() throws IOException {
        Files.writeString(file, "{corrupto");

        newStore().putGroup(new GroupRecord("g", ArtifactScope.GLOBAL, "c"));

        assertTrue(newStore().group("g").isPresent());
        try(Stream<Path> backups = Files.list(appHome.resolve("backups"))) {
            assertEquals(List.of("{corrupto"), backups.map(p -> {
                try { return Files.readString(p); } catch(IOException e) { throw new IllegalStateException(e); }
            }).toList());
        }
    }

    @Test
    void storeNeverWritesInsideTheProjectNorTouchesArtifacts() throws IOException {
        Path artifact = Files.writeString(project.resolve("agente.md"), "contenido\r\n");
        MetadataStore store = newStore();

        store.putArtifact(artifact, "g", "contenido\r\n", LinkState.LINKED);
        store.markDetached(artifact);
        Files.delete(file);

        assertTrue(newStore().artifact(artifact).isEmpty());
        assertEquals("contenido\r\n", Files.readString(artifact));
        try(Stream<Path> files = Files.walk(project)) {
            assertEquals(List.of(project, artifact), files.sorted().toList());
        }
    }

    @Test
    void hashIgnoresLineEndingStyleButNotContent() {
        assertEquals(ContentHash.of("a\nb\n"), ContentHash.of("a\r\nb\r\n"));
        assertFalse(ContentHash.of("a\nb\n").equals(ContentHash.of("a\nc\n")));
        assertEquals(64, ContentHash.of("x").length());
    }

    @Test
    void recordsKeepTheirPathKeyNormalizedWithForwardSlashes() {
        MetadataStore store = newStore();
        Path withDots = project.resolve("sub").resolve("..").resolve("a.md");
        store.putArtifact(withDots, "g", "x", LinkState.LINKED);

        ArtifactRecord record = store.artifact(project.resolve("a.md")).orElseThrow();

        assertEquals(MetadataStore.key(project.resolve("a.md")), record.path());
        assertFalse(record.path().contains("\\"));
    }

    // --- hook sdd-specs-only ---

    private static final byte[] HOOK_CONTENT = "@echo off\nrem hook\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @Test
    void hookIsRegisteredInItsGlobalGroupAndQueriedByAbsolutePath() {
        Path hook = tmp.resolve(".claude/hooks/sdd-specs-only.bat");

        newStore().putHook(hook, HOOK_CONTENT);

        MetadataStore reopened = newStore();
        ArtifactRecord record = reopened.hook(hook).orElseThrow();
        assertEquals(MetadataStore.key(hook), record.path());
        assertEquals(GroupIds.HOOK, record.groupId());
        assertEquals(LinkState.LINKED, record.state());
        assertEquals(ContentHash.of("@echo off\nrem hook\n"), record.hash());
        assertEquals(64, record.hash().length());

        GroupRecord group = reopened.group(GroupIds.HOOK).orElseThrow();
        assertEquals(ArtifactScope.GLOBAL, group.scope());
        assertEquals("", group.sharedBody());
    }

    @Test
    void hashIgnoresCrlfVersusLf() {
        Path hook = tmp.resolve(".claude/hooks/sdd-specs-only.bat");
        byte[] crlf = "@echo off\r\nrem hook\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        MetadataStore store = newStore();
        store.putHook(hook, HOOK_CONTENT);
        String lf = store.hook(hook).orElseThrow().hash();
        store.putHook(hook, crlf);

        assertEquals(lf, store.hook(hook).orElseThrow().hash());
        assertEquals(lf, ContentHash.ofUtf8(crlf));
    }

    @Test
    void oneGroupCoversTheHookOfEveryProviderAndExistingEntriesAreUntouched() {
        MetadataStore store = newStore();
        Path agent = tmp.resolve(".claude/agents/sdd-agent.md");
        store.putGroup(new GroupRecord("global/agent/sdd-agent", ArtifactScope.GLOBAL, "cuerpo"));
        store.putArtifact(agent, "global/agent/sdd-agent", "agente", LinkState.LINKED);
        ArtifactRecord before = store.artifact(agent).orElseThrow();

        Path claude = tmp.resolve(".claude/hooks/sdd-specs-only.sh");
        Path codex = tmp.resolve(".codex/hooks/sdd-specs-only.sh");
        store.putHook(claude, HOOK_CONTENT);
        store.putHook(codex, HOOK_CONTENT);

        assertEquals(2, store.artifactsOfGroup(GroupIds.HOOK).size());
        assertEquals(before, store.artifact(agent).orElseThrow());
        assertEquals("cuerpo", store.group("global/agent/sdd-agent").orElseThrow().sharedBody());
    }

    @Test
    void hookQueryIgnoresAPathRegisteredWithAnotherGroup() {
        MetadataStore store = newStore();
        Path path = tmp.resolve(".claude/hooks/sdd-specs-only.bat");
        store.putArtifact(path, "global/agent/otro", "x", LinkState.LINKED);

        assertTrue(store.hook(path).isEmpty());
        assertTrue(store.artifact(path).isPresent());
    }

    @Test
    void anUnreadableStoreGivesNoHookRecord() throws IOException {
        Files.writeString(file, "{ esto no es json");

        assertTrue(newStore().hook(tmp.resolve(".claude/hooks/sdd-specs-only.bat")).isEmpty());
    }
}
