package xyz.wagyourtail.jvmdg.j16.stub.java_base;

import xyz.wagyourtail.jvmdg.stub.UnixSocketAddress;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.SocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Adapter for {@code java.net.UnixDomainSocketAddress} (JEP 380, absent before
 * Java 16). Mirrors the JDK semantics: the path must come from the default
 * file system and be a JDK implementation ({@code getClassLoader() == null} is
 * the pre-module equivalent of the JDK's module check).
 */
@JEP(380)
@Adapter("java/net/UnixDomainSocketAddress")
public final class J_N_UnixDomainSocketAddress extends SocketAddress implements UnixSocketAddress {

    private static final long serialVersionUID = 92902496589351288L;

    private final transient Path path;

    private J_N_UnixDomainSocketAddress(Path path) {
        this.path = path;
    }

    public static J_N_UnixDomainSocketAddress of(String pathname) {
        return of(Paths.get(pathname));
    }

    public static J_N_UnixDomainSocketAddress of(Path path) {
        if (path.getFileSystem() != FileSystems.getDefault()
            || path.getClass().getClassLoader() != null) {
            throw new IllegalArgumentException();
        }
        return new J_N_UnixDomainSocketAddress(path);
    }

    public Path getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof J_N_UnixDomainSocketAddress
            && path.equals(((J_N_UnixDomainSocketAddress) obj).path);
    }

    @Override
    public String toString() {
        return path.toString();
    }

    private Object writeReplace() throws ObjectStreamException {
        return new Ser(path.toString());
    }

    private static final class Ser implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String pathname;

        Ser(String pathname) {
            this.pathname = pathname;
        }

        private Object readResolve() throws ObjectStreamException {
            return J_N_UnixDomainSocketAddress.of(pathname);
        }

    }

}
