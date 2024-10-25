package xyz.wagyourtail.jvmdg.j22.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Module {
    @Stub(ref = @Ref("java/lang/Module"))
    public static boolean isNativeAccessEnabled(Module module) {
        return true;
    }

}
