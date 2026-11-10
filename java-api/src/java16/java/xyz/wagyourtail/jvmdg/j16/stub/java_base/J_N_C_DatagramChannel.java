package xyz.wagyourtail.jvmdg.j16.stub.java_base;

import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.spi.SelectorProvider;

@JEP(380)
public class J_N_C_DatagramChannel {

    @Stub(ref = @Ref("Ljava/nio/channels/DatagramChannel;"))
    public static DatagramChannel open(ProtocolFamily family) throws IOException {
        return J_N_C_S_SelectorProvider.openDatagramChannel(SelectorProvider.provider(), family);
    }

}
