package xyz.wagyourtail.jvmdg.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncUtils {
    // a dedicated pool rather than commonPool: jar-walk tasks block on
    // subtask futures, and a private pool keeps that nested blocking off
    // the shared pool other libraries in an embedded JVM (gradle daemon,
    // ide) may be using
    private static final ForkJoinPool pool = new ForkJoinPool(Math.min(Math.max(1, Runtime.getRuntime().availableProcessors() - 1), Short.MAX_VALUE), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    @SafeVarargs
    public static <T> Future<List<T>> waitForFutures(Future<T>... futures) {
        for (Future<T> future : futures) {
            if (future == null) {
                throw new NullPointerException();
            }
        }
        return waitForFutures(new ArrayDeque<>(Arrays.asList(futures)));
    }

    public static <T> Future<List<T>> waitForFutures(final Queue<Future<T>> futures) {
        final AtomicReference<List<T>> result = new AtomicReference<>();
        return new Future<List<T>>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return result.get() != null;
            }

            @Override
            public List<T> get() throws InterruptedException, ExecutionException {
                try {
                    return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public List<T> get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                List<T> done = result.get();
                if (done != null) {
                    return done;
                }
                synchronized (futures) {
                    List<T> list = new ArrayList<>();
                    long nanos = unit.toNanos(timeout);
                    // saturate: avoid overflow when adding nanoTime for huge timeouts (e.g. Long.MAX_VALUE)
                    long deadline = nanos >= Long.MAX_VALUE / 2 ? Long.MAX_VALUE : System.nanoTime() + nanos;
                    while (!futures.isEmpty()) {
                        Future<T> future = futures.poll();
                        long remaining = deadline - System.nanoTime();
                        list.add(future.get(Math.max(remaining, 0), TimeUnit.NANOSECONDS));
                    }
                    result.compareAndSet(null, list);
                    return result.get();
                }
            }
        };
    }

    public static <T> Future<Void> forEachAsync(final Collection<T> paths, final IOConsumer<T> fileVisitor) {
        List<CompletableFuture<Void>> futures = new ArrayList<>(paths.size());
        for (final T path : paths) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    fileVisitor.accept(path);
                } catch (IOException e) {
                    Utils.<RuntimeException>sneakyThrow(e);
                }
            }, pool));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public static Future<Void> runAll(Runnable... runnables) {
        List<CompletableFuture<Void>> futures = new ArrayList<>(runnables.length);
        for (final Runnable runnable : runnables) {
            futures.add(CompletableFuture.runAsync(runnable, pool));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public static Future<Void> visitPathsAsync(final Path start, final IOFunction<Path, Boolean> folderVisitor, final IOConsumer<Path> fileVisitor) throws IOException {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        // walkFileTree rather than Files.walk — preVisitDirectory's
        // SKIP_SUBTREE pruning has no stream equivalent
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, BasicFileAttributes attrs) throws IOException {
                if (folderVisitor != null) {
                    if (!folderVisitor.apply(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                if (dir.equals(start)) {
                    return FileVisitResult.CONTINUE;
                }
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        visitPathsAsync(dir, folderVisitor, fileVisitor).get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } catch (ExecutionException | IOException e) {
                        Utils.<RuntimeException>sneakyThrow(e);
                    }
                }, pool));
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        fileVisitor.accept(file);
                    } catch (IOException e) {
                        Utils.<RuntimeException>sneakyThrow(e);
                    }
                }, pool));
                return FileVisitResult.CONTINUE;
            }
        });
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

}
