package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.nio.Buffer;

public class J_N_Buffer {

    @Stub(opcVers = Opcodes.V9, subtypes = true)
    public static Buffer slice(Buffer buffer) {
        throw new UnsupportedOperationException("JVMDowngrader: Not implemented yet.");
    }

    @Stub(opcVers = Opcodes.V9, subtypes = true)
    public static Buffer duplicate(Buffer buffer) {
        throw new UnsupportedOperationException("JVMDowngrader: Not implemented yet.");
    }

    @Stub(opcVers = Opcodes.V9, subtypesOnly = true, returnDecendant = true)
    public static Buffer position(Buffer buffer, int newPosition) {
        return buffer.position(newPosition);
    }

    @Stub(opcVers = Opcodes.V9, subtypesOnly = true, returnDecendant = true)
    public static Buffer limit(Buffer buffer, int newLimit) {
        return buffer.limit(newLimit);
    }

    @Stub(opcVers = Opcodes.V9, subtypesOnly = true, returnDecendant = true)
    public static Buffer mark(Buffer buffer) {
        return buffer.mark();
    }

    @Stub(opcVers = Opcodes.V9, subtypesOnly = true, returnDecendant = true)
    public static Buffer reset(Buffer buffer) {
        return buffer.reset();
    }

    @Stub(opcVers = Opcodes.V9, subtypesOnly = true, returnDecendant = true)
    public static Buffer clear(Buffer buffer) {
        return buffer.clear();
    }

    @Stub(opcVers = Opcodes.V9, subtypesOnly = true, returnDecendant = true)
    public static Buffer flip(Buffer buffer) {
        return buffer.flip();
    }

    @Stub(opcVers = Opcodes.V9, subtypesOnly = true, returnDecendant = true)
    public static Buffer rewind(Buffer buffer) {
        return buffer.rewind();
    }

}
