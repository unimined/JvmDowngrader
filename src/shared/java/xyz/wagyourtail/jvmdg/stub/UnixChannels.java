package xyz.wagyourtail.jvmdg.stub;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Entry point for AF_UNIX channel support on pre-16 targets (JEP 380). There
 * is no pure-Java way to create unix-domain sockets on those JVMs, so the
 * family is delegated to an {@link OptionalDependency}-flagged backend —
 * currently junixsocket — or fails with an
 * {@link UnsupportedOperationException} (matching the spec's declared behavior
 * for unsupported protocol families).
 */
public final class UnixChannels {

    /** the api jar's junixsocket backend — resolved by name so shared code needs no optional deps */
    private static final String BACKEND = "xyz.wagyourtail.jvmdg.impl.JunixsocketBackend";

    private UnixChannels() {
    }

    public static SocketChannel openSocketChannel() throws IOException {
        return backend("Unix domain socket channels").openSocketChannel();
    }

    public static ServerSocketChannel openServerSocketChannel() throws IOException {
        return backend("Unix domain socket channels").openServerSocketChannel();
    }

    public static DatagramChannel openDatagramChannel() throws IOException {
        return backend("Unix domain datagram channels").openDatagramChannel();
    }

    private static UnixChannelBackend backend(String capability) throws UnsupportedOperationException {
        UnixChannelBackend backend = OptionalDelegate.get(BACKEND, UnixChannelBackend.class);
        if (backend == null) {
            throw OptionalDelegate.unsupported(BACKEND, capability);
        }
        return backend;
    }

    /**
     * @return whether {@code family} names the unix-domain protocol family —
     * checked by {@code name()} so both the injected real constant and the
     * fallback fake satisfy it.
     */
    public static boolean isUnixFamily(java.net.ProtocolFamily family) {
        return "UNIX".equals(family.name());
    }

}
