package xyz.wagyourtail.jvmdg.compile.jar;

import xyz.wagyourtail.jvmdg.compile.OutputUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Collects the outputs of a {@link FusedJarRun}: byte emissions, lazy
 * {@link Files#copy} passthroughs, and directory entries. Byte puts win over
 * copies at the same path — matching the legacy {@code writes}/{@code copies}
 * split where a transformed artifact always shadows a copied original at the
 * same archive location.
 * <p>
 * {@link #flush(Path)} performs a single sorted write pass (deterministic
 * entry order) then pins every timestamp, so identical inputs produce
 * byte-identical outputs.
 */
public final class JarEntrySink {

    private final Set<String> dirs = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final ConcurrentMap<String, byte[]> writes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Path> copies = new ConcurrentHashMap<>();

    /** Schedules a directory to exist in the output. */
    public void mkdir(String rel) {
        dirs.add(rel);
    }

    /** Emits entry bytes at {@code rel} — overwrites any prior put/copy at that path. */
    public void put(String rel, byte[] data) {
        copies.remove(rel);
        writes.put(rel, data);
    }

    /** Emits only if no byte write claims {@code rel} yet — the defer-to-lower-version rule for shaded api emits. Copies never block: a byte write always shadows a copy at the same path. */
    public void putIfAbsent(String rel, byte[] data) {
        writes.putIfAbsent(rel, data);
    }

    /** Passes a source file through unchanged — filled only while no byte emission claims {@code rel}. */
    public void copy(String rel, Path source) {
        if (!writes.containsKey(rel)) {
            copies.put(rel, source);
        }
    }

    /** Writes everything to {@code outRoot} in sorted order and pins timestamps. */
    public void flush(Path outRoot) throws IOException {
        List<String> sortedDirs = new ArrayList<>(dirs);
        Collections.sort(sortedDirs);
        for (String d : sortedDirs) {
            Files.createDirectories(outRoot.resolve(d));
        }
        List<String> order = new ArrayList<>(writes.size() + copies.size());
        order.addAll(writes.keySet());
        order.addAll(copies.keySet());
        Collections.sort(order);
        for (String rel : order) {
            Path p = outRoot.resolve(rel);
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            byte[] data = writes.get(rel);
            if (data != null) {
                Files.write(p, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                Files.copy(copies.get(rel), p, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        OutputUtils.pinTimestamps(outRoot);
    }

}
