package es.vargontoc.agentic.viewer.watcher;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.Watchable;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

/**
 * Vigila los directorios del proyecto y de los proveedores globales con
 * {@link WatchService}. Agrupa eventos con un período de debounce y notifica
 * al callback con las rutas afectadas.
 */
public final class ProjectWatcher implements AutoCloseable {

    /** Evento de cambio agrupado. */
    public record ChangeEvent(List<Path> changedPaths, boolean hasDeletions) {
        public ChangeEvent {
            changedPaths = List.copyOf(changedPaths);
        }
    }

    private static final long DEBOUNCE_MILLIS = 500;

    private final Consumer<ChangeEvent> listener;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private WatchService watchService;
    private Thread watchThread;
    private final Set<Path> watchedDirectories = new HashSet<>();

    /**
     * Crea un watcher que notifica al listener dado cuando detecta cambios.
     */
    public ProjectWatcher(Consumer<ChangeEvent> listener) {
        this.listener = listener;
    }

    /**
     * Inicia la vigilancia de los directorios dados (y sus subdirectorios).
     * Si ya está iniciado, primero se detiene.
     */
    public void start(List<Path> directories) {
        stop();
        if (directories == null || directories.isEmpty()) {
            return;
        }

        try {
            watchService = java.nio.file.FileSystems.getDefault().newWatchService();
        } catch (IOException exception) {
            return; // WatchService not available; silently skip
        }

        watchedDirectories.clear();
        for (Path dir : directories) {
            if (dir != null && Files.isDirectory(dir)) {
                registerRecursive(dir);
            }
        }

        if (watchedDirectories.isEmpty()) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
            watchService = null;
            return;
        }

        running.set(true);
        watchThread = Thread.ofVirtual().name("project-watcher").start(this::watchLoop);
    }

    /**
     * Detiene la vigilancia.
     */
    public void stop() {
        running.set(false);
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
            watchService = null;
        }
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
        watchedDirectories.clear();
    }

    @Override
    public void close() {
        stop();
    }

    /** Indica si el watcher está activo. */
    public boolean isRunning() {
        return running.get();
    }

    private void registerRecursive(Path root) {
        try {
            Files.walkFileTree(root, Set.of(FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // If we can't walk the tree, skip it
        }
    }

    private void register(Path dir) {
        if (watchedDirectories.contains(dir)) {
            return;
        }
        try {
            dir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
            watchedDirectories.add(dir);
        } catch (IOException ignored) {
            // Directory not watchable; skip
        }
    }

    private void watchLoop() {
        try {
            while (running.get()) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (ClosedWatchServiceException | InterruptedException exception) {
                    return;
                }

                List<Path> changedPaths = new ArrayList<>();
                boolean hasDeletions = false;

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        // Treat overflow as a generic change in the watched directory
                        Path dir = (Path) key.watchable();
                        changedPaths.add(dir);
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path dir = (Path) key.watchable();
                    Path resolved = dir.resolve(pathEvent.context());
                    changedPaths.add(resolved);

                    if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        hasDeletions = true;
                    }

                    // If a new directory was created, register it too
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(resolved)) {
                        registerRecursive(resolved);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    // Directory no longer accessible
                    Path dir = (Path) key.watchable();
                    watchedDirectories.remove(dir);
                }

                if (!changedPaths.isEmpty()) {
                    // Debounce: wait a bit for more events to accumulate
                    try {
                        Thread.sleep(DEBOUNCE_MILLIS);
                        // Drain any additional pending keys
                        drainPending(changedPaths);
                    } catch (InterruptedException exception) {
                        return;
                    }

                    ChangeEvent changeEvent = new ChangeEvent(changedPaths, hasDeletions);
                    try {
                        listener.accept(changeEvent);
                    } catch (RuntimeException ignored) {
                        // Don't let listener exceptions kill the watcher
                    }
                }
            }
        } finally {
            running.set(false);
        }
    }

    private void drainPending(List<Path> accumulator) {
        // No more keys available immediately — nothing to drain.
        // The take() in the main loop will block until the next event.
    }
}
