package xyz.wagyourtail.jvmdg.j15.stub.java_base;


import xyz.wagyourtail.jvmdg.stub.UnixChannels;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;

public class J_N_C_S_SelectorProvider {

    @Stub
    public static SocketChannel openSocketChannel(SelectorProvider provider, ProtocolFamily family) throws IOException {
        Objects.requireNonNull(family);
        if (family == StandardProtocolFamily.INET || family == StandardProtocolFamily.INET6) {
            return provider.openSocketChannel();
        }
        if (UnixChannels.isUnixFamily(family)) {
            // UNIX needs a backend on pre-16 targets — junixsocket or UOE
            return UnixChannels.openSocketChannel();
        }
        throw new UnsupportedOperationException("Protocol family not supported");
    }

    @Stub
    public static ServerSocketChannel openServerSocketChannel(SelectorProvider provider, ProtocolFamily family) throws IOException {
        Objects.requireNonNull(family);
        if (family == StandardProtocolFamily.INET || family == StandardProtocolFamily.INET6) {
            return provider.openServerSocketChannel();
        }
        if (UnixChannels.isUnixFamily(family)) {
            return UnixChannels.openServerSocketChannel();
        }
        throw new UnsupportedOperationException("Protocol family not supported");
    }

}
