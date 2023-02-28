package xyz.wagyourtail.jvmdg.internal.mods.stub._15;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;

public class J_N_C_S_SelectorProvider {

    @Stub(value = JavaVersion.VERSION_15, desc = "Ljava/nio/channels/spi/SelectorProvider;")
    public static SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
        Objects.requireNonNull(family);
        throw new UnsupportedOperationException("Protocol family not supported");
    }

    @Stub(value = JavaVersion.VERSION_15, desc = "Ljava/nio/channels/spi/SelectorProvider;")
    public static SocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
        Objects.requireNonNull(family);
        throw new UnsupportedOperationException("Protocol family not supported");
    }
}
