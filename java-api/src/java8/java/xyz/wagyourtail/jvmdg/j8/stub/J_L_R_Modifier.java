package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Modifier;

public class J_L_R_Modifier {

    @Stub(ref = @Ref("Ljava/lang/reflect/Modifier;"))
    public static int parameterModifiers() {
        return Modifier.FINAL;
    }
}
