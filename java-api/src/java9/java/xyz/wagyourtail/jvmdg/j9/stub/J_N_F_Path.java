package xyz.wagyourtail.jvmdg.j9.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;

public class J_N_F_Path {

    private static final MethodHandle resolveString;
    private static final MethodHandle resolveSiblingPath;
    private static final MethodHandle resolveSiblingString;

    static {
        MethodHandle resolveStringTemp;
        MethodHandle resolveSiblingPathTemp;
        MethodHandle resolveSiblingStringTemp;
        try {
            resolveStringTemp = MethodHandles.publicLookup().findVirtual(Path.class, "resolve", MethodType.methodType(Path.class, String.class));
            resolveSiblingPathTemp = MethodHandles.publicLookup().findVirtual(Path.class, "resolveSibling", MethodType.methodType(Path.class, Path.class));
            resolveSiblingStringTemp = MethodHandles.publicLookup().findVirtual(Path.class, "resolveSibling", MethodType.methodType(Path.class, String.class));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        resolveString = resolveStringTemp;
        resolveSiblingPath = resolveSiblingPathTemp;
        resolveSiblingString = resolveSiblingStringTemp;
    }

    // defaults were added in j9, so...

    @Stub(opcVers = Opcodes.V9, subtypes = true)
    public static Path resolve(Path self, String other) throws Throwable {
        try {
            return (Path) resolveString.bindTo(self).invokeExact(other);
        } catch (Throwable throwable) {
            if (throwable instanceof AbstractMethodError) {
                return self.resolve(self.getFileSystem().getPath(other));
            } else {
                throw throwable;
            }
        }
    }

    @Stub(opcVers = Opcodes.V9, subtypes = true)
    public static Path resolveSibling(Path self, Path other) throws Throwable {
        try {
            return (Path) resolveSiblingPath.bindTo(self).invokeExact(other);
        } catch (Throwable throwable) {
            if (throwable instanceof AbstractMethodError) {
                Path parent = self.getParent();
                if (parent == null) {
                    return other;
                }
                return parent.resolve(other);
            } else {
                throw throwable;
            }
        }
    }

    @Stub(opcVers = Opcodes.V9, subtypes = true)
    public static Path resolveSibling(Path self, String other) throws Throwable {
        try {
            return (Path) resolveSiblingString.bindTo(self).invokeExact(other);
        } catch (Throwable throwable) {
            if (throwable instanceof AbstractMethodError) {
                return resolveSibling(self, self.getFileSystem().getPath(other));
            } else {
                throw throwable;
            }
        }
    }

}
