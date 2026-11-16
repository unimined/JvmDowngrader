package xyz.wagyourtail.jvmdg.compile;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * Helpers for deterministic output: fixed timestamps and sorted write order so
 * repeated downgrades of the same input produce byte-identical output.
 */
public final class OutputUtils {

    /** ZIP format cannot represent times before 1980-01-01 (315532800000L epoch millis). */
    public static final FileTime FIXED_TIME = FileTime.fromMillis(315532800000L);

    private OutputUtils() {
    }

    /**
     * Sets every entry under {@code root} (files and directories) to
     * {@link #FIXED_TIME}, making output timestamps reproducible across runs.
     *
     * @param root output root (zip filesystem root or plain directory)
     * @throws IOException if a timestamp cannot be set
     */
    public static void pinTimestamps(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                pinTimestamp(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                if (dir.getNameCount() == 0) return FileVisitResult.CONTINUE; // zipfs root has no entry
                pinTimestamp(dir);
                return FileVisitResult.CONTINUE;
            }

        });
    }

    /**
     * Pins all three timestamps of {@code path} to {@link #FIXED_TIME}.
     * {@code mtime} alone is insufficient — zipfs also emits {@code atime}
     * and {@code ctime} in entry extra fields, so a single
     * {@link Files#setLastModifiedTime} still leaks wall-clock time into
     * the output zip's bytes.
     *
     * @param path entry to pin
     * @throws IOException if a timestamp cannot be set
     */
    public static void pinTimestamp(Path path) throws IOException {
        Files.getFileAttributeView(path, BasicFileAttributeView.class)
            .setTimes(FIXED_TIME, FIXED_TIME, FIXED_TIME);
    }
}
