package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.nio.channels.SelectionKey;

public class J_N_C_SelectionKey {

    @Stub(javaVersion = Opcodes.V11)
    public static int interestOpsOr(SelectionKey key, int ops) {
        synchronized (key) {
            int old = key.interestOps();
            key.interestOps(old | ops);
            return old;
        }
    }

}
