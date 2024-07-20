package xyz.wagyourtail.jvmdg.j15.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_CharSequence {

    @Stub(excludeChild = "java/lang/String")
    public static boolean isEmpty(CharSequence cs) {
        return cs.length() == 0;
    }

}
