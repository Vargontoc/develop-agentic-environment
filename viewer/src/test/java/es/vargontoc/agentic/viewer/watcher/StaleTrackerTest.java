package es.vargontoc.agentic.viewer.watcher;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class StaleTrackerTest {

    @Test
    void initiallyNotStale() {
        StaleTracker tracker = new StaleTracker();
        assertFalse(tracker.isAnyStale());
        assertEquals(0, tracker.size());
    }

    @Test
    void markStaleAddsPaths() {
        StaleTracker tracker = new StaleTracker();
        tracker.markStale(List.of(Path.of("/proj/agents/x.toml")));
        assertTrue(tracker.isAnyStale());
        assertEquals(1, tracker.size());
    }

    @Test
    void isStaleMatchesExactPath() {
        StaleTracker tracker = new StaleTracker();
        Path path = Path.of("/proj/agents/x.toml");
        tracker.markStale(List.of(path));
        assertTrue(tracker.isStale(path.toAbsolutePath().normalize().toString()));
    }

    @Test
    void isStaleMatchesParentDirectory() {
        StaleTracker tracker = new StaleTracker();
        Path dir = Path.of("/proj/agents");
        tracker.markStale(List.of(dir));
        assertTrue(tracker.isStale(dir.resolve("x.toml").toAbsolutePath().normalize().toString()));
    }

    @Test
    void isStaleReturnsFalseForUnrelatedPath() {
        StaleTracker tracker = new StaleTracker();
        tracker.markStale(List.of(Path.of("/proj/agents/x.toml")));
        assertFalse(tracker.isStale(Path.of("/proj/skills/y").toAbsolutePath().normalize().toString()));
    }

    @Test
    void clearResetsState() {
        StaleTracker tracker = new StaleTracker();
        tracker.markStale(List.of(Path.of("/a"), Path.of("/b")));
        assertTrue(tracker.isAnyStale());

        tracker.clear();
        assertFalse(tracker.isAnyStale());
        assertEquals(0, tracker.size());
        assertFalse(tracker.isStale("/a"));
    }

    @Test
    void nullPathIsNotStale() {
        StaleTracker tracker = new StaleTracker();
        tracker.markStale(List.of(Path.of("/a")));
        assertFalse(tracker.isStale(null));
    }
}
