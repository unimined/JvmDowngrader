package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_Boolean {

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Boolean"))
    public static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Boolean"))
    public static boolean logicalAnd(boolean a, boolean b) {
        return a && b;
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Boolean"))
    public static boolean logicalOr(boolean a, boolean b) {
        return a || b;
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Boolean"))
    public static boolean logicalXor(boolean a, boolean b) {
        return a ^ b;
    }

}
