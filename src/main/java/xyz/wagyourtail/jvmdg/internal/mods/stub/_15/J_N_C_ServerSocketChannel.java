package xyz.wagyourtail.jvmdg.internal.mods.stub._15;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

public class J_N_C_ServerSocketChannel {

    @Stub(value = JavaVersion.VERSION_15, desc = "Ljava/nio/channels/ServerSocketChannel;")
    public static ServerSocketChannel open(ProtocolFamily family) throws IOException {
        return SelectorProvider.provider().openServerSocketChannel(family);
    }

}
