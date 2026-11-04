package es.vargontoc.agentic.develop.cli.infrastructure.files;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;

class TextStoreTest {

    @TempDir
    Path tmp;

    Path scope;
    Path backups;
    TextStore store;

    @BeforeEach
    void setUp() throws IOException {
        scope = Files.createDirectories(tmp.resolve("scope"));
        backups = tmp.resolve("backups");
        store = new TextStore(new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX), List.of(scope), backups);
    }

    private TextStore failingStore() {
        return new TextStore(new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX), List.of(scope), backups,
            (source, target) -> { throw new IOException("fallo simulado"); });
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void crlfFileKeepsCrlfAndLeavesARecoverableBackup() throws IOException {
        Path file = scope.resolve("agente.md");
        Files.write(file, bytes("uno\r\ndos\r\n"));

        TextStore.WriteResult result = store.write(file, "uno\ntres\n");

        assertTrue(result.written());
        assertArrayEquals(bytes("uno\r\ntres\r\n"), Files.readAllBytes(file));
        assertNotNull(result.backup());
        assertArrayEquals(bytes("uno\r\ndos\r\n"), Files.readAllBytes(result.backup()));
        assertEquals("\r\n", store.read(file).lineEnding());
    }

    @Test
    void newFilesUseLfAndCreateMissingDirectories() throws IOException {
        Path file = scope.resolve("sub").resolve("dir").resolve("nuevo.md");

        TextStore.WriteResult result = store.write(file, "a\r\nb\r\n");

        assertTrue(result.written());
        assertNull(result.backup());
        assertArrayEquals(bytes("a\nb\n"), Files.readAllBytes(file));
    }

    @Test
    void utf8ContentRoundTrips() {
        Path file = scope.resolve("utf8.md");

        store.write(file, "técnico — año ✓\n");

        assertEquals("técnico — año ✓\n", store.read(file).content());
    }

    @Test
    void writingTheSameContentDoesNothing() throws IOException {
        Path file = scope.resolve("igual.md");
        Files.write(file, bytes("x\r\n"));

        TextStore.WriteResult result = store.write(file, "x\n");

        assertFalse(result.written());
        assertNull(result.backup());
        assertFalse(Files.exists(backups));
    }

    @Test
    void failedWriteLeavesTheOriginalIntactAndNoTemporaryFile() throws IOException {
        Path file = scope.resolve("original.md");
        Files.write(file, bytes("contenido original\n"));

        DomainException e = assertThrows(DomainException.class, () -> failingStore().write(file, "nuevo\n"));

        assertEquals(AppErrorCode.TEXT_WRITE_FAILED, e.code());
        assertArrayEquals(bytes("contenido original\n"), Files.readAllBytes(file));
        try(Stream<Path> files = Files.list(scope)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    @Test
    void pathsOutsideTheAllowedScopesAreRejectedForReadAndWrite() throws IOException {
        Path outside = Files.writeString(tmp.resolve("fuera.md"), "x");

        DomainException write = assertThrows(DomainException.class, () -> store.write(outside, "y"));
        DomainException read = assertThrows(DomainException.class, () -> store.read(outside));

        assertEquals(AppErrorCode.PATH_OUT_OF_SCOPE, write.code());
        assertEquals(AppErrorCode.PATH_OUT_OF_SCOPE, read.code());
        assertEquals("x", Files.readString(outside));
    }

    @Test
    void invalidUtf8CannotBeRead() throws IOException {
        Path file = scope.resolve("binario.md");
        Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28});

        DomainException e = assertThrows(DomainException.class, () -> store.read(file));

        assertEquals(AppErrorCode.TEXT_READ_FAILED, e.code());
    }

    @Test
    void successiveOverwritesKeepDistinctBackups() throws IOException {
        Path file = scope.resolve("hist.md");
        store.write(file, "v1\n");

        Path first = store.write(file, "v2\n").backup();
        Path second = store.write(file, "v3\n").backup();

        assertTrue(!first.equals(second));
        assertEquals("v1\n", Files.readString(first));
        assertEquals("v2\n", Files.readString(second));
    }
}
