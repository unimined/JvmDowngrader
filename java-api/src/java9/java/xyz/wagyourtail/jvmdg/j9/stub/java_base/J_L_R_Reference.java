package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_R_Reference {

    /**
     * The call itself is the barrier — the argument stays strongly
     * reachable for its duration, matching the JDK's own empty body.
     */
    @Stub(ref = @Ref("Ljava/lang/ref/Reference;"))
    public static void reachabilityFence(Object ref) {
    }

}
