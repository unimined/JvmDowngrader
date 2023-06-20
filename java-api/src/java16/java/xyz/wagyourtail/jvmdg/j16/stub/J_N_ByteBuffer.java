package xyz.wagyourtail.jvmdg.j16.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.ByteBuffer;
import java.util.Objects;

public class J_N_ByteBuffer {

    @Stub(opcVers = Opcodes.V16)
    public static ByteBuffer put(ByteBuffer self, int index, ByteBuffer src, int offset, int length) {
        Objects.checkFromIndexSize(index, length, self.limit());
        Objects.checkFromIndexSize(offset, length, src.limit());
        if (src == self && index < offset && offset < index + length) {
            // If the source and target overlap, and the target is
            // "before" the source, then we must copy backwards
            for (int i = length - 1; i >= 0; i--) {
                self.put(index + i, src.get(offset + i));
            }
        } else {
            for (int i = 0; i < length; i++) {
                self.put(index + i, src.get(offset + i));
            }
        }
        return self;
    }

}
