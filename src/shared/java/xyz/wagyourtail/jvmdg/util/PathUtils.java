package xyz.wagyourtail.jvmdg.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ForkJoinPool;

public class PathUtils {
    private static final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    public static void visitPathsAsync(final Path start, final Consumer<Path> fileVisitor) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, BasicFileAttributes attrs) throws IOException {
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            visitPathsAsync(dir, fileVisitor);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) throws IOException {
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        fileVisitor.accept(file);
                    }
                });
                return FileVisitResult.CONTINUE;
            }
        });

    }

}
