package xyz.wagyourtail.jvmdg.j15.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_StrictMath {

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static int absEaxct(int a) {
        return J_L_Math.absExact(a);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static long absExact(long a) {
        return J_L_Math.absExact(a);
    }

}
