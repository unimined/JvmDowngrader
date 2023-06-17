package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_Float {
    // @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Double"))
    public static final int BYTES = Float.SIZE / Byte.SIZE;

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Float"))
    public static boolean isFinite(float f) {
        return Math.abs(f) <= Float.MAX_VALUE;
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Float"))
    public static int hashCode(float value) {
        return Float.floatToIntBits(value);
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Float"))
    public static float sum(float a, float b) {
        return a + b;
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Float"))
    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Float"))
    public static float min(float a, float b) {
        return Math.min(a, b);
    }

}
