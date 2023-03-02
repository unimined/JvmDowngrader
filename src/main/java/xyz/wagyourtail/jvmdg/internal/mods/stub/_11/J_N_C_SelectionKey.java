package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.nio.channels.SelectionKey;

public class J_N_C_SelectionKey {

    @Stub(JavaVersion.VERSION_11)
    public static int interestOpsOr(SelectionKey key, int ops) {
        synchronized (key) {
            int old = key.interestOps();
            key.interestOps(old | ops);
            return old;
        }
    }
}
