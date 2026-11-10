package es.vargontoc.agentic.viewer.watcher;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CacheStoreTest {

    @Test
    void getOrLoadCachesFirstResult() {
        CacheStore store = new CacheStore();
        int[] calls = {0};
        String first = store.getOrLoad("key", () -> {
            calls[0]++;
            return "value";
        });
        String second = store.getOrLoad("key", () -> {
            calls[0]++;
            return "other";
        });

        assertEquals("value", first);
        assertEquals("value", second);
        assertEquals(1, calls[0]);
    }

    @Test
    void invalidateRemovesKey() {
        CacheStore store = new CacheStore();
        store.getOrLoad("key", () -> "value");
        assertEquals(1, store.size());

        store.invalidate("key");
        assertEquals(0, store.size());

        String reloaded = store.getOrLoad("key", () -> "new-value");
        assertEquals("new-value", reloaded);
    }

    @Test
    void invalidatePrefixRemovesMatchingKeys() {
        CacheStore store = new CacheStore();
        store.getOrLoad("describe:/proj/a", () -> "A");
        store.getOrLoad("describe:/proj/b", () -> "B");
        store.getOrLoad("tree:/proj", () -> "T");

        assertEquals(3, store.size());
        store.invalidatePrefix("describe:");
        assertEquals(1, store.size());
    }

    @Test
    void invalidateAllClearsEverything() {
        CacheStore store = new CacheStore();
        store.getOrLoad("a", () -> 1);
        store.getOrLoad("b", () -> 2);
        store.getOrLoad("c", () -> 3);

        store.invalidateAll();
        assertEquals(0, store.size());
    }

    @Test
    void describeKeyNormalizesProject() {
        String global = CacheStore.describeKey("/agents/x", null);
        String project = CacheStore.describeKey("/agents/x", "/my/proj");
        assertNotEquals(global, project);
        assertTrue(global.contains("global"));
        assertTrue(project.contains("/my/proj"));
    }

    @Test
    void statusKeyAndTreeKey() {
        String status = CacheStore.statusKey("/proj");
        String tree = CacheStore.treeKey("/proj");
        assertNotEquals(status, tree);
        assertTrue(status.startsWith("status:"));
        assertTrue(tree.startsWith("tree:"));
    }

    @Test
    void invalidateProjectSoloAfectaASusClaves() {
        CacheStore store = new CacheStore();
        store.getOrLoad(CacheStore.statusKey("/a"), () -> "status-a");
        store.getOrLoad(CacheStore.treeKey("/a"), () -> "tree-a");
        store.getOrLoad(CacheStore.describeKey("/a/x", "/a"), () -> "describe-a");
        store.getOrLoad(CacheStore.describeKey("/a/x", "/b"), () -> "describe-b");
        store.getOrLoad(CacheStore.treeKey("/b"), () -> "tree-b");

        store.invalidateProject("/a");

        assertEquals(2, store.size());
        assertEquals("describe-b", store.getOrLoad(CacheStore.describeKey("/a/x", "/b"), () -> "fallback"));
        assertEquals("tree-b", store.getOrLoad(CacheStore.treeKey("/b"), () -> "fallback"));
    }

    @Test
    void invalidateArtifactConservaOtrasDescripciones() {
        CacheStore store = new CacheStore();
        store.getOrLoad(CacheStore.describeKey("/a/x", "/a"), () -> "x");
        store.getOrLoad(CacheStore.describeKey("/a/y", "/a"), () -> "y");
        store.getOrLoad(CacheStore.treeKey("/a"), () -> "tree");

        store.invalidateArtifact("/a/x", "/a");

        assertEquals(1, store.size());
        assertEquals("y", store.getOrLoad(CacheStore.describeKey("/a/y", "/a"), () -> "fallback"));
    }
}
