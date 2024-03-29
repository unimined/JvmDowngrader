package xyz.wagyourtail.jvmdg.j15.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

public class J_N_C_SocketChannel {

    @Stub(ref = @Ref("Ljava/nio/channels/SocketChannel;"))
    public static SocketChannel open(ProtocolFamily family) throws IOException {
        return J_N_C_S_SelectorProvider.openSocketChannel(SelectorProvider.provider(), family);
    }

}
