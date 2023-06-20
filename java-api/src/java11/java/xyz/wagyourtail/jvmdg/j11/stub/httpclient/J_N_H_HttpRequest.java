package xyz.wagyourtail.jvmdg.j11.stub.httpclient;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

@Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/net/http/HttpRequest;"))
public abstract class J_N_H_HttpRequest {
    public J_N_H_HttpRequest() {
        throw new UnsupportedOperationException("TODO");
    }

    public static Builder newBuilder() {
        throw new UnsupportedOperationException("TODO");
    }

    public abstract Optional<BodyPublisher> bodyPublisher();

    public abstract String method();

    public abstract Optional<Duration> timeout();
    public abstract boolean expectContinue();
    public abstract URI uri();
    public abstract Optional<J_N_H_HttpClient.Version> version();
//    public abstract HttpHeaders headers();

    public final boolean equals(Object obj) {
        if (!(obj instanceof J_N_H_HttpRequest)) {
            return false;
        }
        J_N_H_HttpRequest that = (J_N_H_HttpRequest) obj;
        if (!that.method().equals(this.method())) {
            return false;
        }
        if (!that.uri().equals(this.uri())) {
            return false;
        }
//        if (!that.headers().equals(this.headers())) {
//            return false;
//        }
        return true;
    }

    public final int hashCode() {
        return method().hashCode() + uri().hashCode(); // + headers().hashCode();
    }

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/net/http/HttpRequest$BodyPublisher;"))
    public interface BodyPublisher extends Flow.Publisher<ByteBuffer> {
        long contentLength();
    }

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/net/http/HttpRequest$Builder;"))
    public interface Builder {
        Builder uri(URI uri);
        Builder expectContinue(boolean expectContinue);
        Builder version(J_N_H_HttpClient.Version version);
        Builder header(String name, String value);
        Builder headers(String... headers);
        Builder timeout(Duration duration);
        Builder setHeader(String name, String value);
        Builder GET();
        Builder POST(BodyPublisher publisher);
        Builder PUT(BodyPublisher publisher);
        Builder DELETE();
        Builder method(String name, BodyPublisher publisher);
        J_N_H_HttpRequest build();
        Builder copy();
    }

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/net/http/HttpRequest$BodyPublishers;"))
    public static class BodyPublishers {
        private BodyPublishers() {
        }

        public static BodyPublisher fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher) {
            throw new UnsupportedOperationException("TODO");
        }

        public static BodyPublisher fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher, long contentLength) {
            throw new UnsupportedOperationException("TODO");
        }

        public static BodyPublisher ofString(String s) {
            return ofString(s, StandardCharsets.UTF_8);
        }

        public static BodyPublisher ofString(String s, Charset charset) {
            throw new UnsupportedOperationException("TODO");
        }

        public static BodyPublisher ofInputStream(Supplier<? extends InputStream> streamSupplier) {
            throw new UnsupportedOperationException("TODO");
        }

        public static BodyPublisher ofByteArray(byte[] bytes) {
            throw new UnsupportedOperationException("TODO");
        }

        public static BodyPublisher ofByteArray(byte[] bytes, int offset, int length) {
            throw new UnsupportedOperationException("TODO");
        }

        public static BodyPublisher ofFile(Path file) {
            throw new UnsupportedOperationException("TODO");
        }

        public static BodyPublisher ofByteArrays(Iterable<byte[]> byteArrays) {
            throw new UnsupportedOperationException("TODO");
        }

        public static BodyPublisher noBody() {
            throw new UnsupportedOperationException("TODO");
        }

        public static BodyPublisher concat(BodyPublisher... publishers) {
            throw new UnsupportedOperationException("TODO");
        }
    }
}
