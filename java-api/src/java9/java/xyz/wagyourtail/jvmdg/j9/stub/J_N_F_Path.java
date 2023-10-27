package xyz.wagyourtail.jvmdg.j9.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class J_N_F_Path {

    @Stub(wasAbstract = true)
    public static Path resolve(Path self, String other) throws Throwable {
        return self.resolve(self.getFileSystem().getPath(other));
    }

    @Stub(wasAbstract = true)
    public static Path resolveSibling(Path self, Path other) throws Throwable {
        Path parent = self.getParent();
        if (parent == null) {
            return other;
        }
        return parent.resolve(other);
    }

    @Stub(wasAbstract = true)
    public static Path resolveSibling(Path self, String other) throws Throwable {
        return resolveSibling(self, self.getFileSystem().getPath(other));
    }

    /**
     * missing default before j9
     */
    @Stub(wasAbstract = true)
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
