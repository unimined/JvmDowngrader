package xyz.wagyourtail.jvmdg.j11.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Character {

    @Stub(ref = @Ref("java/lang/Character"))
    public static String toString(int codePoint) {
        return new String(Character.toChars(codePoint));
    }

    // TODO: Unicode codespaces

}
