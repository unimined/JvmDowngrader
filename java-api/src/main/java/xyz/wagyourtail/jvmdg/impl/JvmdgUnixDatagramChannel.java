package xyz.wagyourtail.jvmdg.impl;

import xyz.wagyourtail.jvmdg.stub.UnixSocketAddress;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * {@link DatagramChannel} wrapping a junixsocket-provided channel,
 * translating addresses at the boundary.
 */
public class JvmdgUnixDatagramChannel extends DatagramChannel {

    private final DatagramChannel inner;

    protected JvmdgUnixDatagramChannel(SelectorProvider provider, DatagramChannel inner) {
        super(provider);
        this.inner = inner;
    }

    @Override
    public DatagramChannel bind(SocketAddress local) throws IOException {
        inner.bind(JunixsocketBackend.toUnixAddress(local));
        return this;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return JunixsocketBackend.fromUnixAddress(inner.getLocalAddress());
    }

    @Override
    public <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException {
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
    public DatagramSocket socket() {
        return inner.socket();
    }

    @Override
    public boolean isConnected() {
        return inner.isConnected();
    }

    @Override
    public DatagramChannel connect(SocketAddress remote) throws IOException {
        inner.connect(JunixsocketBackend.toUnixAddress(remote));
        return this;
    }

    @Override
    public DatagramChannel disconnect() throws IOException {
        inner.disconnect();
        return this;
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return JunixsocketBackend.fromUnixAddress(inner.getRemoteAddress());
    }

    @Override
    public SocketAddress receive(ByteBuffer dst) throws IOException {
        return JunixsocketBackend.fromUnixAddress(inner.receive(dst));
    }

    @Override
    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        return inner.send(src, JunixsocketBackend.toUnixAddress(target));
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
    public MembershipKey join(InetAddress group, NetworkInterface interf) throws IOException {
        return wrapKey(inner.join(group, interf));
    }

    @Override
    public MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source) throws IOException {
        return wrapKey(inner.join(group, interf, source));
    }

    private MembershipKey wrapKey(MembershipKey key) {
        return key == null ? null : new JvmdgMembershipKey(key);
    }

    /**
     * Maps {@code getChannel()} back to this wrapper so the inner channel
     * doesn't leak. (AF_UNIX can't multicast in practice — the backend will
     * throw before this is ever constructed.)
     */
    private class JvmdgMembershipKey extends MembershipKey {

        private final MembershipKey innerKey;

        JvmdgMembershipKey(MembershipKey innerKey) {
            this.innerKey = innerKey;
        }

        @Override
        public boolean isValid() {
            return innerKey.isValid();
        }

        @Override
        public void drop() {
            innerKey.drop();
        }

        @Override
        public MembershipKey block(InetAddress source) throws IOException {
            innerKey.block(source);
            return this;
        }

        @Override
        public MembershipKey unblock(InetAddress source) {
            innerKey.unblock(source);
            return this;
        }

        @Override
        public DatagramChannel channel() {
            return JvmdgUnixDatagramChannel.this;
        }

        @Override
        public InetAddress group() {
            return innerKey.group();
        }

        @Override
        public NetworkInterface networkInterface() {
            return innerKey.networkInterface();
        }

        @Override
        public InetAddress sourceAddress() {
            return innerKey.sourceAddress();
        }

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
