package xyz.wagyourtail.jvmdg.j15.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Math {

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static int absExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("Overflow to represent absolute value of Integer.MIN_VALUE");
        }
        return Math.abs(a);
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long absExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("Overflow to represent absolute value of Long.MIN_VALUE");
        }
        return Math.abs(a);
    }

}
