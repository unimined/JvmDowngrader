package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.channels.SelectionKey;

public class J_N_C_SelectionKey {

    @Stub
    public static int interestOpsOr(SelectionKey key, int ops) {
        synchronized (key) {
            int old = key.interestOps();
            key.interestOps(old | ops);
            return old;
        }
    }

}
