package es.vargontoc.agentic.viewer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecentProjectsStoreTest {

    @TempDir
    Path tmp;

    private Path project(String name) throws IOException {
        return Files.createDirectories(tmp.resolve(name));
    }

    @Test
    void recuerdaElMasRecientePrimeroYSinDuplicados() throws IOException {
        RecentProjectsStore store = new RecentProjectsStore(tmp.resolve("recent.json"));
        Path alfa = project("alfa");
        Path beta = project("beta");

        store.remember(alfa);
        store.remember(beta);
        store.remember(alfa);

        assertEquals(List.of(alfa, beta), store.list());
        assertEquals(alfa, store.lastExisting().orElseThrow());
    }

    @Test
    void limitaLaListaAlMaximo() throws IOException {
        RecentProjectsStore store = new RecentProjectsStore(tmp.resolve("recent.json"));
        for (int i = 0; i < RecentProjectsStore.MAX + 3; i++) {
            store.remember(project("p" + i));
        }

        assertEquals(RecentProjectsStore.MAX, store.list().size());
        assertEquals(tmp.resolve("p" + (RecentProjectsStore.MAX + 2)), store.list().getFirst());
    }

    @Test
    void ocultaLosProyectosQueYaNoExistenPeroConservaElResto() throws IOException {
        RecentProjectsStore store = new RecentProjectsStore(tmp.resolve("recent.json"));
        Path existente = project("existe");
        Path borrado = project("borrado");

        store.remember(borrado);
        store.remember(existente);
        Files.delete(borrado);

        assertEquals(List.of(existente), store.list());
        assertEquals(existente, store.lastExisting().orElseThrow());
    }

    @Test
    void eliminarYLimpiarActualizanLaLista() throws IOException {
        RecentProjectsStore store = new RecentProjectsStore(tmp.resolve("recent.json"));
        Path alfa = project("alfa");
        Path beta = project("beta");
        store.remember(alfa);
        store.remember(beta);

        store.remove(alfa);
        assertEquals(List.of(beta), store.list());

        store.clear();
        assertTrue(store.list().isEmpty());
        assertFalse(Files.exists(tmp.resolve("recent.json")));
    }

    @Test
    void unFicheroCorruptoSeTrataComoListaVacia() throws IOException {
        Path file = tmp.resolve("recent.json");
        Files.writeString(file, "[no es json");
        RecentProjectsStore store = new RecentProjectsStore(file);

        assertTrue(store.list().isEmpty());
        store.remember(project("alfa"));
        assertEquals(1, store.list().size());
    }
}
