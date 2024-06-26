package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Character {

    @Stub(ref = @Ref("java/lang/Character"))
    public static final int BYTES = Character.SIZE / Byte.SIZE;

    @Stub(ref = @Ref("java/lang/Character"))
    public static int hashCode(char value) {
        return value;
    }


}
