package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class J_N_F_Path {

    @Stub(abstractDefault = true)
    public static File toFile(Path path) {
        if (path.getFileSystem() == FileSystems.getDefault()) {
            return new File(path.toString());
        }
        throw new UnsupportedOperationException("Path not associated with default filesystem.");
    }

    @Stub(abstractDefault = true)
    public static boolean startsWith(Path path, String other) {
        return path.startsWith(path.getFileSystem().getPath(other));
    }

    @Stub(abstractDefault = true)
    public static boolean endsWith(Path path, String other) {
        return path.endsWith(path.getFileSystem().getPath(other));
    }

    @Stub(abstractDefault = true)
    public static WatchKey register(Path path, WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return path.register(watcher, events, new WatchEvent.Modifier[0]);
    }

    @Stub(abstractDefault = true)
    public static Path resolve(Path self, String other) throws Throwable {
        return self.resolve(self.getFileSystem().getPath(other));
    }

    @Stub(abstractDefault = true)
    public static Path resolveSibling(Path self, Path other) throws Throwable {
        Path parent = self.getParent();
        if (parent == null) {
            return other;
        }
        return parent.resolve(other);
    }

    @Stub(abstractDefault = true)
    public static Path resolveSibling(Path self, String other) throws Throwable {
        return resolveSibling(self, self.getFileSystem().getPath(other));
    }

    /**
     * missing default before j9
     */
    @Stub(abstractDefault = true)
    public static Iterator<Path> iterator(Path self) {
        return new Iterator<Path>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return (i < self.getNameCount());
            }

            @Override
            public Path next() {
                if (i < self.getNameCount()) {
                    Path result = self.getName(i);
                    i++;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

}
