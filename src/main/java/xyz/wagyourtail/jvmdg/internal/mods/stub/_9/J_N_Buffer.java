package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class J_N_Buffer {

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static Buffer slice() {
        throw new UnsupportedOperationException("JVMDowngrader: Not implemented yet.");
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static Buffer duplicate() {
        throw new UnsupportedOperationException("JVMDowngrader: Not implemented yet.");
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true, returnDecendant = true)
    public static ByteBuffer position(ByteBuffer buffer, int newPosition) {
        return (ByteBuffer) ((Buffer) buffer).position(newPosition);
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true, returnDecendant = true)
    public static ByteBuffer limit(ByteBuffer buffer, int newLimit) {
        return (ByteBuffer) ((Buffer) buffer).limit(newLimit);
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true, returnDecendant = true)
    public static ByteBuffer mark(ByteBuffer buffer) {
        return (ByteBuffer) ((Buffer) buffer).mark();
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true, returnDecendant = true)
    public static ByteBuffer reset(ByteBuffer buffer) {
        return (ByteBuffer) ((Buffer) buffer).reset();
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true, returnDecendant = true)
    public static ByteBuffer clear(ByteBuffer buffer) {
        return (ByteBuffer) ((Buffer) buffer).clear();
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true, returnDecendant = true)
    public static ByteBuffer flip(ByteBuffer buffer) {
        return (ByteBuffer) ((Buffer) buffer).flip();
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true, returnDecendant = true)
    public static ByteBuffer rewind(ByteBuffer buffer) {
        return (ByteBuffer) ((Buffer) buffer).rewind();
    }

    // TODO: template other buffer types too
}
