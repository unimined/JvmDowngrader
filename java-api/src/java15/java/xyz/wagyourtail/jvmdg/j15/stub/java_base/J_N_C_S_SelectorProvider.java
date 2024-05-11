package xyz.wagyourtail.jvmdg.j15.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;

public class J_N_C_S_SelectorProvider {

    @Stub
    public static SocketChannel openSocketChannel(SelectorProvider provider, ProtocolFamily family) throws IOException {
        Objects.requireNonNull(family);
        throw new UnsupportedOperationException("Protocol family not supported");
    }

    @Stub
    public static ServerSocketChannel openServerSocketChannel(SelectorProvider provider, ProtocolFamily family) throws IOException {
        Objects.requireNonNull(family);
        throw new UnsupportedOperationException("Protocol family not supported");
    }

}
