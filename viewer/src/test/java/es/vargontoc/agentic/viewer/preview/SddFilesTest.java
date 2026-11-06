package es.vargontoc.agentic.viewer.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SddFilesTest {

    @TempDir
    Path temp;

    @Test
    void listaSoloLosFicherosDeTextoOrdenados() throws IOException {
        Path sdd = temp.resolve(".sdd");
        Files.createDirectories(sdd);
        Files.writeString(sdd.resolve("team.md"), "# Team");
        Files.writeString(sdd.resolve("architecture.md"), "# Arquitectura");
        Files.writeString(sdd.resolve("producto.md"), "# Producto");
        Files.write(sdd.resolve("nota.bin"), new byte[] { 1, 2, 3 });

        List<Path> files = SddFiles.list(sdd);

        assertEquals(List.of("architecture.md", "producto.md", "team.md"),
            files.stream().map(Path::getFileName).map(Path::toString).toList());
    }

    @Test
    void listaVacioCuandoNoExisteElDirectorio() {
        assertTrue(SddFiles.list(temp.resolve("no-existe")).isEmpty());
    }

    @Test
    void leeUnFicheroComoUtf8() throws IOException {
        Path sdd = temp.resolve(".sdd");
        Files.createDirectories(sdd);
        Path team = sdd.resolve("team.md");
        Files.writeString(team, "## Módulos\n- facturacion | - | -");

        assertEquals("## Módulos\n- facturacion | - | -", SddFiles.read(sdd, team));
    }

    @Test
    void rechazaUnaRutaFueraDeSdd() throws IOException {
        Path sdd = temp.resolve(".sdd");
        Files.createDirectories(sdd);
        Path outside = temp.resolve("otro.md");
        Files.writeString(outside, "fuera");

        assertThrows(SddFiles.SddReadException.class, () -> SddFiles.read(sdd, outside));
    }

    @Test
    void informaDeContenidoNoUtf8() throws IOException {
        Path sdd = temp.resolve(".sdd");
        Files.createDirectories(sdd);
        Path file = sdd.resolve("roto.md");
        Files.write(file, new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x41 });

        assertThrows(SddFiles.SddReadException.class, () -> SddFiles.read(sdd, file));
    }

    @Test
    void informaDeFicheroInexistente() throws IOException {
        Path sdd = temp.resolve(".sdd");
        Files.createDirectories(sdd);

        assertThrows(SddFiles.SddReadException.class, () -> SddFiles.read(sdd, sdd.resolve("nada.md")));
    }

    @Test
    void noEscribeNadaAlLeer() throws IOException {
        Path sdd = temp.resolve(".sdd");
        Files.createDirectories(sdd);
        Path team = sdd.resolve("team.md");
        Files.writeString(team, "original", StandardCharsets.UTF_8);

        SddFiles.read(sdd, team);

        assertEquals("original", Files.readString(team));
    }
}
