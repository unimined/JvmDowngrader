package xyz.wagyourtail.jvmdg.j15.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

public class J_N_C_ServerSocketChannel {

    @Stub(opcVers = Opcodes.V15, ref = @Ref("Ljava/nio/channels/ServerSocketChannel;"))
    public static ServerSocketChannel open(ProtocolFamily family) throws IOException {
        return J_N_C_S_SelectorProvider.openServerSocketChannel(SelectorProvider.provider(), family);
    }

}
