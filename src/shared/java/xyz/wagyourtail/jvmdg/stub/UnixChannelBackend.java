package xyz.wagyourtail.jvmdg.stub;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Contract for the AF_UNIX channel backend. Implemented by the
 * {@code @OptionalDependency}-flagged {@code JunixsocketBackend} in the api
 * jar and resolved by name through {@link OptionalDelegate} — shared code
 * cannot reference the implementation (it lives where the optional
 * dependency's types are on the compile classpath).
 */
public interface UnixChannelBackend {

    SocketChannel openSocketChannel() throws IOException;

    ServerSocketChannel openServerSocketChannel() throws IOException;

    DatagramChannel openDatagramChannel() throws IOException;

}
