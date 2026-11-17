package xyz.wagyourtail.jvmdg.compile.jar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * One jar entry flowing through a {@link FusedJarRun} step chain. {@link #name}
 * is the {@code /}-normalized archive-relative path; contents are either carried
 * ({@link #data}) or referenced lazily ({@link #source}) — passthrough entries
 * keep their source path so they are copied without ever materializing bytes.
 * {@link #slot} records the input multi-release position ({@code 0} for jar
 * root, {@code n} for {@code META-INF/versions/<n>/}) for steps that index
 * classes per slot.
 */
public final class JarEntry {

    public final String name;
    public final Path source;
    public final byte[] data;
    public final int slot;

    public JarEntry(String name, Path source) {
        this(name, source, null, 0);
    }

    private JarEntry(String name, Path source, byte[] data, int slot) {
        this.name = name;
        this.source = source;
        this.data = data;
        this.slot = slot;
    }

    /** Reads the entry bytes — from {@link #data} when carried, else from {@link #source}. */
    public byte[] bytes() throws IOException {
        return data != null ? data : Files.readAllBytes(source);
    }

    /** Same entry with carried bytes instead of a source reference (transformed content). */
    public JarEntry withData(byte[] data) {
        return new JarEntry(name, null, data, slot);
    }

    /** Same entry under a different output path. */
    public JarEntry renamed(String name) {
        return new JarEntry(name, source, data, slot);
    }

    /** Same entry reading from a different source file (multi-release prefer-highest swap). */
    public JarEntry withSource(Path source) {
        return new JarEntry(name, source, data, slot);
    }

    /** Same entry tagged with a different input multi-release slot. */
    public JarEntry atSlot(int slot) {
        return new JarEntry(name, source, data, slot);
    }

}
