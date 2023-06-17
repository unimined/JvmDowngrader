package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_Character {

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Character"))
    public static final int BYTES = Character.SIZE / Byte.SIZE;

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Character"))
    public static int hashCode(char value) {
        return value;
    }


}
