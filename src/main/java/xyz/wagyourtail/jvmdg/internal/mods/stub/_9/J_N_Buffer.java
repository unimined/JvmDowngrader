package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class J_N_Buffer {

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static Buffer slice(Buffer buffer) {
        throw new UnsupportedOperationException("JVMDowngrader: Not implemented yet.");
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static Buffer duplicate(Buffer buffer) {
        throw new UnsupportedOperationException("JVMDowngrader: Not implemented yet.");
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypesOnly = true, returnDecendant = true)
    public static Buffer position(Buffer buffer, int newPosition) {
        return buffer.position(newPosition);
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypesOnly = true, returnDecendant = true)
    public static Buffer limit(Buffer buffer, int newLimit) {
        return buffer.limit(newLimit);
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypesOnly = true, returnDecendant = true)
    public static Buffer mark(Buffer buffer) {
        return buffer.mark();
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypesOnly = true, returnDecendant = true)
    public static Buffer reset(Buffer buffer) {
        return buffer.reset();
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypesOnly = true, returnDecendant = true)
    public static Buffer clear(Buffer buffer) {
        return buffer.clear();
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypesOnly = true, returnDecendant = true)
    public static Buffer flip(Buffer buffer) {
        return buffer.flip();
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypesOnly = true, returnDecendant = true)
    public static Buffer rewind(Buffer buffer) {
        return buffer.rewind();
    }

}
