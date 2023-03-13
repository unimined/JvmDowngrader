package xyz.wagyourtail.jvmdg.j14.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_StrictMath {

    @Stub(javaVersion = Opcodes.V14, ref = @Ref("Ljava/lang/StrictMath;"))
    public static int incrementExact(int i) {
        return Math.incrementExact(i);
    }

    @Stub(javaVersion = Opcodes.V14, ref = @Ref("Ljava/lang/StrictMath;"))
    public static long incrementExact(long i) {
        return Math.incrementExact(i);
    }

    @Stub(javaVersion = Opcodes.V14, ref = @Ref("Ljava/lang/StrictMath;"))
    public static int decrementExact(int i) {
        return Math.decrementExact(i);
    }

    @Stub(javaVersion = Opcodes.V14, ref = @Ref("Ljava/lang/StrictMath;"))
    public static long decrementExact(long i) {
        return Math.decrementExact(i);
    }

    @Stub(javaVersion = Opcodes.V14, ref = @Ref("Ljava/lang/StrictMath;"))
    public static int negateExact(int i) {
        return Math.negateExact(i);
    }

    @Stub(javaVersion = Opcodes.V14, ref = @Ref("Ljava/lang/StrictMath;"))
    public static long negateExact(long i) {
        return Math.negateExact(i);
    }

}
