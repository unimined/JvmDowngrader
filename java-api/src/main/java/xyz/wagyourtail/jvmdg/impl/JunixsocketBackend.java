package xyz.wagyourtail.jvmdg.impl;

import org.newsclub.net.unix.AFUNIXSelectorProvider;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import xyz.wagyourtail.jvmdg.stub.OptionalDependency;
import xyz.wagyourtail.jvmdg.stub.UnixChannelBackend;
import xyz.wagyourtail.jvmdg.stub.UnixSocketAddress;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

/**
 * {@link OptionalDependency}-flagged {@link UnixChannelBackend} providing
 * AF_UNIX channels via junixsocket when it is on the classpath. All
 * junixsocket interaction is typed code confined to this class plus the
 * {@code JvmdgUnix*Channel} wrappers; instantiation resolves (and touches)
 * the junixsocket provider so broken/absent installs count as unavailable.
 */
@OptionalDependency(value = {
    "org.newsclub.net.unix.AFUNIXSelectorProvider",
    "org.newsclub.net.unix.AFUNIXSocketAddress"
}, name = "junixsocket", maven = "com.kohlschutter.junixsocket:junixsocket-core")
public class JunixsocketBackend implements UnixChannelBackend {

    private final AFUNIXSelectorProvider provider = AFUNIXSelectorProvider.provider();

    @Override
    public SocketChannel openSocketChannel() throws IOException {
        return new JvmdgUnixSocketChannel(provider, provider.openSocketChannel());
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return new JvmdgUnixServerSocketChannel(provider, provider.openServerSocketChannel());
    }

    @Override
    public DatagramChannel openDatagramChannel() throws IOException {
        return new JvmdgUnixDatagramChannel(provider, provider.openDatagramChannel());
    }

    /**
     * Translate a consumer-visible socket address (the adapted
     * {@code UnixDomainSocketAddress}) to junixsocket's address type. Foreign
     * address types pass through so junixsocket reports its own error.
     */
    static SocketAddress toUnixAddress(SocketAddress address) throws IOException {
        if (address instanceof UnixSocketAddress) {
            Path path = ((UnixSocketAddress) address).getPath();
            return path == null ? null : new AFUNIXSocketAddress(path.toFile());
        }
        return address;
    }

    /**
     * Translate a junixsocket address back to the consumer-visible adapter
     * type (resolved lazily — keeping the no-version-dir-imports invariant
     * of the main source set).
     */
    static SocketAddress fromUnixAddress(SocketAddress address) throws IOException {
        if (address instanceof AFUNIXSocketAddress) {
            SocketAddress adapted = adaptedUnixAddress(((AFUNIXSocketAddress) address).getFile().toPath());
            return adapted != null ? adapted : address;
        }
        return address;
    }

    private static Method unixAddressOf;
    private static boolean unixAddressResolved = false;

    static synchronized SocketAddress adaptedUnixAddress(Path path) {
        if (!unixAddressResolved) {
            unixAddressResolved = true;
            try {
                Class<?> adapter = Class.forName(
                    "xyz.wagyourtail.jvmdg.j16.stub.java_base.J_N_UnixDomainSocketAddress");
                unixAddressOf = adapter.getDeclaredMethod("of", Path.class);
            } catch (Throwable ignored) {
            }
        }
        if (unixAddressOf == null) {
            return null;
        }
        try {
            return (SocketAddress) unixAddressOf.invoke(null, path);
        } catch (Throwable t) {
            throw new IllegalStateException("unable to construct adapted UnixDomainSocketAddress", t);
        }
    }

}
