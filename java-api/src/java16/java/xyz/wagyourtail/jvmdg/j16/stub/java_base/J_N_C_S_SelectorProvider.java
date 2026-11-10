package xyz.wagyourtail.jvmdg.j16.stub.java_base;

import xyz.wagyourtail.jvmdg.stub.UnixChannels;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;

@JEP(380)
public class J_N_C_S_SelectorProvider {

    @Stub
    public static DatagramChannel openDatagramChannel(SelectorProvider provider, ProtocolFamily family) throws IOException {
        Objects.requireNonNull(family);
        if (family == StandardProtocolFamily.INET || family == StandardProtocolFamily.INET6) {
            return provider.openDatagramChannel();
        }
        if (UnixChannels.isUnixFamily(family)) {
            // UNIX needs a backend on pre-16 targets — junixsocket or UOE
            return UnixChannels.openDatagramChannel();
        }
        throw new UnsupportedOperationException("Protocol family not supported");
    }

}
