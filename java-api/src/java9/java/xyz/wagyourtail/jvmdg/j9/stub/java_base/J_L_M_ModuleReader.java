package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Adapter("java/lang/module/ModuleReader")
public interface J_L_M_ModuleReader extends Closeable {
    /**
     * Finds a resource, returning a URI to the resource in the module.
     *
     * <p> If the module reader can determine that the name locates a directory
     * then the resulting URI will end with a slash ('/'). </p>
     *
     * @param name The name of the resource to open for reading
     * @return A URI to the resource; an empty {@code Optional} if the resource
     * is not found or a URI cannot be constructed to locate the
     * resource
     * @throws IOException       If an I/O error occurs or the module reader is closed
     * @throws SecurityException If denied by the security manager
     * @see ClassLoader#getResource(String)
     */
    Optional<URI> find(String name) throws IOException;

    default Optional<InputStream> open(String name) throws IOException {
        Optional<URI> optionalURI = find(name);
        if (!optionalURI.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(optionalURI.get().toURL().openStream());
    }

    default Optional<ByteBuffer> read(String name) throws IOException {
        Optional<InputStream> optionalInputStream = open(name);
        if (!optionalInputStream.isPresent()) {
            return Optional.empty();
        }
        try (InputStream in = optionalInputStream.get()) {
            return Optional.of(ByteBuffer.wrap(J_I_InputStream.readAllBytes(in)));
        }
    }

    default void release(ByteBuffer bb) {
        Objects.requireNonNull(bb);
    }

    Stream<String> list() throws IOException;

}
