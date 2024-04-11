package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

public class J_U_Objects {

    @Stub(ref = @Ref("java/util/Objects"))
    public static String toIdentityString(Object o) {
        Objects.requireNonNull(o);
        return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

}
