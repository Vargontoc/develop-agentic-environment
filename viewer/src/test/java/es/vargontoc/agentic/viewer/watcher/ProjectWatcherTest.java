package es.vargontoc.agentic.viewer.watcher;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectWatcherTest {

    @TempDir
    Path tempDir;

    @Test
    void startAndStopLifecycle() throws Exception {
        AtomicReference<ProjectWatcher.ChangeEvent> received = new AtomicReference<>();
        ProjectWatcher watcher = new ProjectWatcher(received::set);

        assertFalse(watcher.isRunning());
        watcher.start(List.of(tempDir));
        assertTrue(watcher.isRunning());

        watcher.stop();
        assertFalse(watcher.isRunning());
    }

    @Test
    void detectsFileCreation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ProjectWatcher.ChangeEvent> received = new AtomicReference<>();
        ProjectWatcher watcher = new ProjectWatcher(event -> {
            received.set(event);
            latch.countDown();
        });

        watcher.start(List.of(tempDir));
        try {
            // Create a file to trigger the watcher
            Path newFile = tempDir.resolve("test.txt");
            Files.writeString(newFile, "hello");

            // Wait for the event (with debounce, it may take ~600ms)
            boolean received_event = latch.await(5, TimeUnit.SECONDS);
            assertTrue(received_event, "Should have received a change event");
            assertNotNull(received.get());
            assertFalse(received.get().changedPaths().isEmpty());
        } finally {
            watcher.stop();
        }
    }

    @Test
    void detectsFileDeletion() throws Exception {
        // Create a file first
        Path existingFile = tempDir.resolve("existing.txt");
        Files.writeString(existingFile, "content");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ProjectWatcher.ChangeEvent> received = new AtomicReference<>();
        ProjectWatcher watcher = new ProjectWatcher(event -> {
            received.set(event);
            latch.countDown();
        });

        watcher.start(List.of(tempDir));
        try {
            Files.delete(existingFile);

            boolean received_event = latch.await(5, TimeUnit.SECONDS);
            assertTrue(received_event, "Should have received a change event");
            assertTrue(received.get().hasDeletions());
        } finally {
            watcher.stop();
        }
    }

    @Test
    void startWithEmptyDirectoriesDoesNotRun() {
        ProjectWatcher watcher = new ProjectWatcher(event -> {});
        watcher.start(List.of());
        assertFalse(watcher.isRunning());
    }

    @Test
    void startWithNullDirectoriesDoesNotRun() {
        ProjectWatcher watcher = new ProjectWatcher(event -> {});
        watcher.start(null);
        assertFalse(watcher.isRunning());
    }

    @Test
    void closeStopsWatcher() throws Exception {
        ProjectWatcher watcher = new ProjectWatcher(event -> {});
        watcher.start(List.of(tempDir));
        assertTrue(watcher.isRunning());
        watcher.close();
        assertFalse(watcher.isRunning());
    }
}
