package xyz.wagyourtail.jvmdg.impl;

import xyz.wagyourtail.jvmdg.stub.UnixSocketAddress;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * {@link SocketChannel} wrapping a junixsocket-provided channel, translating
 * {@code UnixDomainSocketAddress}-adapter addresses at the boundary
 * (junixsocket's {@code connect} requires its own address type).
 */
public class JvmdgUnixSocketChannel extends SocketChannel {

    private final SocketChannel inner;

    protected JvmdgUnixSocketChannel(SelectorProvider provider, SocketChannel inner) {
        super(provider);
        this.inner = inner;
    }

    @Override
    public SocketChannel bind(SocketAddress local) throws IOException {
        inner.bind(JunixsocketBackend.toUnixAddress(local));
        return this;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return JunixsocketBackend.fromUnixAddress(inner.getLocalAddress());
    }

    @Override
    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
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
    public SocketChannel shutdownInput() throws IOException {
        inner.shutdownInput();
        return this;
    }

    @Override
    public SocketChannel shutdownOutput() throws IOException {
        inner.shutdownOutput();
        return this;
    }

    @Override
    public Socket socket() {
        return inner.socket();
    }

    @Override
    public boolean isConnected() {
        return inner.isConnected();
    }

    @Override
    public boolean isConnectionPending() {
        return inner.isConnectionPending();
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        return inner.connect(JunixsocketBackend.toUnixAddress(remote));
    }

    @Override
    public boolean finishConnect() throws IOException {
        return inner.finishConnect();
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return JunixsocketBackend.fromUnixAddress(inner.getRemoteAddress());
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return inner.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return inner.read(dsts, offset, length);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return inner.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return inner.write(srcs, offset, length);
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
