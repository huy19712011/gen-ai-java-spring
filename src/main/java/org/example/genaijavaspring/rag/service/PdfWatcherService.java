package org.example.genaijavaspring.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.example.genaijavaspring.rag.config.data.RagConfigData;
import org.example.genaijavaspring.rag.exception.RagException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class PdfWatcherService implements SmartLifecycle {

    private final RagIngestionService ingestion;
    private final RagConfigData ragConfig;
    private final Executor watchLoop;
    private final ScheduledExecutorService debounce;

    private volatile boolean running = false;
    private java.nio.file.WatchService watchService;
    private final Map<Path, ScheduledFuture<?>> scheduled = new java.util.concurrent.ConcurrentHashMap<>();


    public PdfWatcherService(RagIngestionService ingestion,
                             RagConfigData ragConfig,
                             @Qualifier("traceableWatchLoopExecutor") Executor watchLoop,
                             @Qualifier("traceableScheduledExecutorService") ScheduledExecutorService debounce) {
        this.ingestion = ingestion;
        this.ragConfig = ragConfig;
        this.watchLoop = watchLoop;
        this.debounce = debounce;
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public void start() {
        try {
            Path dir = resolveWatchedDir();       // the directory, not the *.pdf glob
            log.info("Starting PDF folder watcher for dir: {}", dir);
            this.watchService = dir.getFileSystem().newWatchService();
            dir.register(watchService,
                    java.nio.file.StandardWatchEventKinds.ENTRY_CREATE,
                    java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY,
                    java.nio.file.StandardWatchEventKinds.ENTRY_DELETE);
            running = true;
            watchLoop.execute(() -> {
                try {
                    log.info("PDF watcher loop started for dir: {}", dir);
                    this.watchDir();
                } catch (Exception e) {
                    log.error("PDF watcher loop terminated with error", e);
                }
            });
        } catch (Exception e) {
            throw new RagException("Failed to start PDF watcher", e);
        }
    }

    private Path resolveWatchedDir() {
        // Supported forms:
        //   file:///D:/rag-files/qa-over-internal-docs/*.pdf  -> D:/rag-files/qa-over-internal-docs
        //   file:./rag/qa-over-internal-docs/*.pdf            -> ./rag/qa-over-internal-docs
        //   file:/Users/Shared/rag-files/*.pdf                -> /Users/Shared/rag-files
        String location = ragConfig.getPdf().getPath()
                .replaceFirst("^file:", "")
                .replaceFirst("^/+(?=[A-Za-z]:)", ""); // "///D:/x" -> "D:/x" (Windows drive letter)

        // '*' is not a legal Windows path character, so cut the glob off before building a Path
        int wildcard = location.indexOf('*');
        if (wildcard >= 0) {
            int sep = Math.max(location.lastIndexOf('/', wildcard), location.lastIndexOf('\\', wildcard));
            String dir = sep >= 0 ? location.substring(0, sep + 1) : "";
            return java.nio.file.Paths.get(dir).toAbsolutePath().normalize();
        }

        // No glob: a directory is watched directly, a file is watched via its parent
        java.nio.file.Path path = java.nio.file.Paths.get(location).toAbsolutePath().normalize();
        return java.nio.file.Files.isDirectory(path) ? path : path.getParent();
    }

    private void watchDir() {
        while (running) {
            try {
                WatchKey key = watchService.take();
                Path dir = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == java.nio.file.StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Path relative = ((java.nio.file.WatchEvent<Path>) event).context();
                    Path path = dir.resolve(relative);

                    // Only care about .pdf files
                    if (!relative.toString().toLowerCase().endsWith(".pdf")) {
                        continue;
                    }

                    // Debounce per file (e.g., 1s)
                    scheduled.compute(path, (p, pending) -> {
                        if (pending != null) pending.cancel(false);  // cancel any timer already queued for this file
                        return debounce.schedule(() -> handleEvent(kind, p), 1, java.util.concurrent.TimeUnit.SECONDS); // schedule a new one and run only if no new events for 1s
                    });
                }
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("WatchKey no longer valid for {}", dir);
                    break; // or try to re-register the directory
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Watcher is interrupted!");
                break;
            } catch (Exception e) {
                log.warn("Error in watcher loop!", e);
            }
        }
    }

    private void handleEvent(java.nio.file.WatchEvent.Kind<?> kind, Path path) {
        try {
            log.info("Received event:{} -> for file:{}", kind.name(), path.getFileName());
            if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_DELETE) {
                // remove from store
                ingestion.deleteBySource(path.getFileName().toString());
            } else {
                // create/modify → upsert this one file
                ingestion.upsertOneByPath(path);
            }
        } catch (Exception e) {
            log.error("Could not handle event:{} -> for file:{}", kind.name(), path.getFileName());
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (
                IOException ignored) {
        } finally {
            debounce.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
