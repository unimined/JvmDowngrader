package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.function.Predicate;

public class J_U_F_Predicate {

    @Stub(ref = @Ref("Ljava/util/function/Predicate;"))
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }

}
