package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
import es.vargontoc.agentic.develop.cli.domain.metadata.ContentHash;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupIds;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver.Os;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.HookResource;

class HookInstallerTest {

    @TempDir
    Path tmp;

    Path appHome;
    Path providerHome;

    @BeforeEach
    void setUp() throws IOException {
        appHome = Files.createDirectories(tmp.resolve("app-home"));
        providerHome = Files.createDirectories(tmp.resolve("home").resolve(".claude"));
    }

    private PathResolver paths(Os os) {
        return new PathResolver(Map.of("HOME", tmp.toString(), "USERPROFILE", tmp.toString()), os);
    }

    private MetadataStore store(Os os) {
        return new MetadataStore(appHome.resolve("metadata.json"),
            new TextStore(paths(os), List.of(appHome), appHome.resolve("backups")));
    }

    private OperationResult install(Os os) {
        OperationResult result = new OperationResult();
        new HookInstaller(paths(os), store(os)).install(new Provider(ProviderType.CLAUDE, providerHome), result);
        return result;
    }

    private Path hook(String fileName) {
        return providerHome.resolve("hooks").resolve(fileName);
    }

    @Test
    void windowsWithoutHookCreatesTheBatAndNotTheShAndRegistersItsHash() throws IOException {
        OperationResult result = install(Os.WINDOWS);

        assertTrue(Files.isRegularFile(hook("sdd-specs-only.bat")));
        assertFalse(Files.exists(hook("sdd-specs-only.sh")));
        assertArrayEquals(HookResource.content(TargetOs.WINDOWS), Files.readAllBytes(hook("sdd-specs-only.bat")));
        assertEquals(List.of("hooks/sdd-specs-only.bat"), result.created());
        assertTrue(result.warnings().isEmpty());

        var registered = store(Os.WINDOWS).hook(hook("sdd-specs-only.bat")).orElseThrow();
        assertEquals(ContentHash.ofUtf8(Files.readAllBytes(hook("sdd-specs-only.bat"))), registered.hash());
        assertEquals(LinkState.LINKED, registered.state());
    }

    @Test
    void linuxWithoutHookCreatesTheShAndNotTheBatAndRegistersIt() throws IOException {
        OperationResult result = install(Os.LINUX);

        assertTrue(Files.isRegularFile(hook("sdd-specs-only.sh")));
        assertFalse(Files.exists(hook("sdd-specs-only.bat")));
        assertArrayEquals(HookResource.content(TargetOs.LINUX), Files.readAllBytes(hook("sdd-specs-only.sh")));
        assertEquals(List.of("hooks/sdd-specs-only.sh"), result.created());
        assertTrue(store(Os.LINUX).hook(hook("sdd-specs-only.sh")).isPresent());
    }

    @Test
    void linuxHookIsExecutable() {
        assumeTrue(providerHome.getFileSystem().supportedFileAttributeViews().contains("posix"),
            "El sistema de ficheros no es POSIX");

        install(Os.LINUX);

        assertTrue(Files.isExecutable(hook("sdd-specs-only.sh")));
    }

    @Test
    void linuxOnANonPosixFileSystemInstallsAndWarnsThatItCouldNotBeMadeExecutable() {
        assumeTrue(!providerHome.getFileSystem().supportedFileAttributeViews().contains("posix"),
            "Solo aplica a sistemas de ficheros no POSIX");

        OperationResult result = install(Os.LINUX);

        assertTrue(Files.isRegularFile(hook("sdd-specs-only.sh")));
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("executable"));
    }

    @Test
    void hookDeletedByTheUserWithAStaleRecordIsReinstalledAndTheRecordUpdated() throws IOException {
        Path bat = hook("sdd-specs-only.bat");
        store(Os.WINDOWS).putHook(bat, "version antigua\n".getBytes());
        String stale = store(Os.WINDOWS).hook(bat).orElseThrow().hash();
        assertFalse(Files.exists(bat));

        OperationResult result = install(Os.WINDOWS);

        assertEquals(List.of("hooks/sdd-specs-only.bat"), result.created());
        String current = store(Os.WINDOWS).hook(bat).orElseThrow().hash();
        assertEquals(ContentHash.ofUtf8(Files.readAllBytes(bat)), current);
        assertNotEquals(stale, current);
        assertEquals(1, store(Os.WINDOWS).artifactsOfGroup(GroupIds.HOOK).size());
    }

    // --- hook ya instalado: actualización (TASK-005) ---

    private static final String REL_BAT = "hooks/sdd-specs-only.bat";

    // Deja en disco y en el registro una versión anterior del hook, como si la hubiera instalado el CLI
    private Path installOldVersion(String content) throws IOException {
        Path bat = hook("sdd-specs-only.bat");
        Files.createDirectories(bat.getParent());
        Files.writeString(bat, content);
        store(Os.WINDOWS).putHook(bat, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return bat;
    }

    @Test
    void unmodifiedHookWithADifferentBundledVersionIsUpdatedWithoutWarning() throws IOException {
        Path bat = installOldVersion("version antigua\n");

        OperationResult result = install(Os.WINDOWS);

        byte[] bundled = HookResource.content(TargetOs.WINDOWS);
        assertArrayEquals(bundled, Files.readAllBytes(bat));
        assertEquals(ContentHash.ofUtf8(bundled), store(Os.WINDOWS).hook(bat).orElseThrow().hash());
        assertEquals(List.of(REL_BAT), result.created());
        assertTrue(result.warnings().isEmpty());
        try(var files = Files.list(bat.getParent())) {
            assertEquals(List.of("sdd-specs-only.bat"), files.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void unmodifiedHookEqualToTheBundledOneIsNotRewrittenNorWarned() throws IOException {
        install(Os.WINDOWS);
        Path bat = hook("sdd-specs-only.bat");
        var old = java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() - 3_600_000);
        Files.setLastModifiedTime(bat, old);

        OperationResult result = install(Os.WINDOWS);

        assertEquals(old, Files.getLastModifiedTime(bat));
        assertEquals(List.of(REL_BAT), result.skipped());
        assertTrue(result.created().isEmpty());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void hookThatOnlyDiffersFromTheRegisteredOneInLineEndingsIsConsideredUnmodified() throws IOException {
        Path bat = installOldVersion("uno\ndos\n");
        Files.writeString(bat, "uno\r\ndos\r\n");

        OperationResult result = install(Os.WINDOWS);

        assertArrayEquals(HookResource.content(TargetOs.WINDOWS), Files.readAllBytes(bat));
        assertEquals(List.of(REL_BAT), result.created());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void updatedLinuxHookKeepsItsExecutablePermission() throws IOException {
        assumeTrue(providerHome.getFileSystem().supportedFileAttributeViews().contains("posix"),
            "El sistema de ficheros no es POSIX");
        Path sh = hook("sdd-specs-only.sh");
        Files.createDirectories(sh.getParent());
        Files.writeString(sh, "version antigua\n");
        store(Os.LINUX).putHook(sh, "version antigua\n".getBytes());

        install(Os.LINUX);

        assertTrue(Files.isExecutable(sh));
        assertArrayEquals(HookResource.content(TargetOs.LINUX), Files.readAllBytes(sh));
    }

    // --- hook ya instalado: modificado por el usuario (TASK-006) ---

    @Test
    void hookModifiedByTheUserIsKeptAndWarnedAndTheRecordIsNotChanged() throws IOException {
        Path bat = installOldVersion("linea 1\nlinea 2\n");
        String registered = store(Os.WINDOWS).hook(bat).orElseThrow().hash();
        Files.writeString(bat, "linea 1\nlinea 2 cambiada\n");

        OperationResult result = install(Os.WINDOWS);

        assertEquals("linea 1\nlinea 2 cambiada\n", Files.readString(bat));
        assertEquals(registered, store(Os.WINDOWS).hook(bat).orElseThrow().hash());
        assertEquals(List.of(REL_BAT), result.skipped());
        assertTrue(result.created().isEmpty());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).startsWith(REL_BAT));
        assertTrue(result.warnings().get(0).contains("modificado por el usuario"));
    }

    @Test
    void modifiedHookIsNotAnErrorAndTheOtherProvidersAreStillInstalled() throws IOException {
        Path bat = installOldVersion("original\n");
        Files.writeString(bat, "modificado\n");
        Path otherHome = Files.createDirectories(tmp.resolve("home").resolve(".codex"));

        OperationResult result = new OperationResult();
        HookInstaller installer = new HookInstaller(paths(Os.WINDOWS), store(Os.WINDOWS));
        installer.install(new Provider(ProviderType.CLAUDE, providerHome), result);
        installer.install(new Provider(ProviderType.CODEX, otherHome), result);

        assertEquals("modificado\n", Files.readString(bat));
        assertTrue(Files.exists(otherHome.resolve("hooks").resolve("sdd-specs-only.bat")));
        assertEquals(1, result.warnings().size());
    }

    // --- hook ya instalado: sin registro (TASK-007) ---

    @Test
    void hookWithoutARecordIsKeptWarnedAndNoRecordIsCreated() throws IOException {
        Path bat = hook("sdd-specs-only.bat");
        Files.createDirectories(bat.getParent());
        Files.writeString(bat, "de otra persona\n");

        OperationResult result = install(Os.WINDOWS);

        assertEquals("de otra persona\n", Files.readString(bat));
        assertTrue(store(Os.WINDOWS).hook(bat).isEmpty());
        assertEquals(List.of(REL_BAT), result.skipped());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("no se puede comprobar si fue modificado"));
    }

    @Test
    void hookWithoutARecordWarnsEvenWhenItEqualsTheBundledOneAndWarnsAgainOnTheNextRun() throws IOException {
        Path bat = hook("sdd-specs-only.bat");
        Files.createDirectories(bat.getParent());
        Files.write(bat, HookResource.content(TargetOs.WINDOWS));

        OperationResult first = install(Os.WINDOWS);
        OperationResult second = install(Os.WINDOWS);

        assertEquals(1, first.warnings().size());
        assertEquals(1, second.warnings().size());
        assertTrue(store(Os.WINDOWS).hook(bat).isEmpty());
    }

    @Test
    void anUnreadableStoreIsTreatedAsNoRecord() throws IOException {
        Path bat = installOldVersion("version antigua\n");
        Files.writeString(appHome.resolve("metadata.json"), "{ esto no es json");

        OperationResult result = install(Os.WINDOWS);

        assertEquals("version antigua\n", Files.readString(bat));
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("no se puede comprobar si fue modificado"));
    }

    @Test
    void aRecordOfAnotherGroupAtTheHookPathIsTreatedAsNoRecord() throws IOException {
        Path bat = hook("sdd-specs-only.bat");
        Files.createDirectories(bat.getParent());
        Files.writeString(bat, "x\n");
        store(Os.WINDOWS).putArtifact(bat, "global/agent/otro", "x\n", LinkState.LINKED);

        OperationResult result = install(Os.WINDOWS);

        assertEquals("x\n", Files.readString(bat));
        assertEquals(1, result.warnings().size());
    }

    @Test
    void otherScriptsInTheHooksFolderAreLeftAlone() throws IOException {
        Files.createDirectories(hook("x").getParent());
        Files.writeString(hook("mi-script.bat"), "propio\n");

        install(Os.WINDOWS);

        assertEquals("propio\n", Files.readString(hook("mi-script.bat")));
        assertTrue(Files.exists(hook("sdd-specs-only.bat")));
    }

    @Test
    void aHooksFolderThatIsASymlinkOutsideTheProviderFolderInstallsNothing() throws IOException {
        Path outside = Files.createDirectories(tmp.resolve("outside"));
        try {
            Files.createSymbolicLink(providerHome.resolve("hooks"), outside);
        } catch(IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "No se pueden crear enlaces simbólicos en este entorno");
        }

        OperationResult result = install(Os.LINUX);

        assertTrue(result.created().isEmpty());
        assertEquals(1, result.warnings().size());
        try(var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
    }
}
