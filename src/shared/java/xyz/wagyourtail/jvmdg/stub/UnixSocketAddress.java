package xyz.wagyourtail.jvmdg.stub;

import java.nio.file.Path;

/**
 * Marker for the adapted {@code java.net.UnixDomainSocketAddress} so the
 * shared unix-channel machinery (which cannot see the version-dir adapter
 * class) can extract the socket path. Implemented by
 * {@code J_N_UnixDomainSocketAddress} in java16.
 */
public interface UnixSocketAddress {

    /**
     * @return the socket file path, mirroring
     * {@code UnixDomainSocketAddress.getPath()}.
     */
    Path getPath();

}
