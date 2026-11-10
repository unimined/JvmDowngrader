package xyz.wagyourtail.jvmdg.impl;

import xyz.wagyourtail.jvmdg.stub.UnixSocketAddress;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * {@link ServerSocketChannel} wrapping a junixsocket-provided channel,
 * translating addresses at the boundary and re-wrapping accepted channels.
 */
public class JvmdgUnixServerSocketChannel extends ServerSocketChannel {

    private final ServerSocketChannel inner;
    private final SelectorProvider backendProvider;

    protected JvmdgUnixServerSocketChannel(SelectorProvider provider, ServerSocketChannel inner) {
        super(provider);
        this.backendProvider = provider;
        this.inner = inner;
    }

    @Override
    public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        inner.bind(JunixsocketBackend.toUnixAddress(local), backlog);
        return this;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return JunixsocketBackend.fromUnixAddress(inner.getLocalAddress());
    }

    @Override
    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        inner.setOption(name, value);
        return this;
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return inner.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return inner.supportedOptions();
    }

    @Override
    public ServerSocket socket() {
        return inner.socket();
    }

    @Override
    public SocketChannel accept() throws IOException {
        SocketChannel accepted = inner.accept();
        return accepted == null ? null : new JvmdgUnixSocketChannel(backendProvider, accepted);
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        inner.close();
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        inner.configureBlocking(block);
    }

}
