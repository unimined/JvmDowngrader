package xyz.wagyourtail.jvmdg.j11.stub.java_net_http;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.util.Consumer;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.Adapter;

import javax.net.ssl.SSLSession;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

@Adapter("Ljava/net/http/HttpResponse;")
public interface J_N_H_HttpResponse<T> {

    int statusCode();

    J_N_H_HttpRequest request();

    Optional<J_N_H_HttpResponse<T>> previousResponse();

    J_N_H_HttpHeaders headers();

    T body();

    Optional<SSLSession> sslSession();

    URI uri();

    J_N_H_HttpClient.Version version();

    @Adapter("Ljava/net/http/HttpResponse$BodySubscriber;")
    interface BodySubscriber<T> extends Flow.Subscriber<List<ByteBuffer>> {

        CompletionStage<T> getBody();

    }

    @Adapter("Ljava/net/http/HttpResponse$ResponseInfo;")
    interface ResponseInfo {
        int statusCode();

        J_N_H_HttpHeaders headers();

        J_N_H_HttpClient.Version version();
    }

    @Adapter("Ljava/net/http/HttpResponse$BodyHandler;")
    interface BodyHandler<T> {

        BodySubscriber<T> apply(ResponseInfo responseInfo);

    }

    @Adapter("Ljava/net/http/HttpResponse$BodyHandlers;")
    class BodyHandlers {
        private static Charset charsetFrom(J_N_H_HttpHeaders headers) {
            throw MissingStubError.create();
        }

        public static BodyHandler<Void> fromSubscriber(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            Objects.requireNonNull(subscriber);
            return (info) -> BodySubscribers.fromSubscriber(subscriber);
        }

        public static <S extends Flow.Subscriber<? super List<ByteBuffer>>, T> BodyHandler<Void> fromSubscriber(Flow.Subscriber<? super List<ByteBuffer>> subscriber, Function<? super S,? extends T> finisher) {
            Objects.requireNonNull(subscriber);
            Objects.requireNonNull(finisher);
            return (info) -> BodySubscribers.fromSubscriber(subscriber, finisher);
        }

        public static BodyHandler<Void> fromLineSubscriber(Flow.Subscriber<? super String> subscriber) {
            Objects.requireNonNull(subscriber);
            return (info) -> BodySubscribers.fromLineSubscriber(subscriber, (s) -> null, charsetFrom(info.headers()), null);
        }

        public static BodyHandler<Void> fromLineSubscriber(
            Flow.Subscriber<? super String> subscriber,
            Function<? super Flow.Subscriber<? super String>,? extends Void> finisher,
            String lineSeparator
        ) {
            Objects.requireNonNull(subscriber);
            Objects.requireNonNull(finisher);
            return (info) -> BodySubscribers.fromLineSubscriber(subscriber, finisher, charsetFrom(info.headers()), lineSeparator);
        }

        public static BodyHandler<Void> discarding() {
            return (info) -> BodySubscribers.discarding();
        }

        public static <U> BodyHandler<U> replacing(U value) {
            Objects.requireNonNull(value);
            return (info) -> BodySubscribers.replacing(value);
        }

        public static BodyHandler<String> ofString(Charset charset) {
            Objects.requireNonNull(charset);
            return (info) -> BodySubscribers.ofString(charset);
        }

        public static BodyHandler<Path> ofFile(Path file, OpenOption... options) {
            Objects.requireNonNull(file);
            return (info) -> BodySubscribers.ofFile(file, options);
        }

        public static BodyHandler<Path> ofFile(Path file) {
            return ofFile(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }

        public static BodyHandler<Path> ofFileDownload(Path dir, OpenOption... options) {
            Objects.requireNonNull(dir);
            return (info) -> {
                throw MissingStubError.create();
            };
        }

        public static BodyHandler<InputStream> ofInputStream() {
            return (info) -> BodySubscribers.ofInputStream();
        }

        public static BodyHandler<Stream<String>> ofLines() {
            return (info) -> BodySubscribers.ofLines(charsetFrom(info.headers()));
        }

        public static BodyHandler<Void> ofByteArrayConsumer(Consumer<Optional<byte[]>> consumer) {
            Objects.requireNonNull(consumer);
            return (info) -> BodySubscribers.ofByteArrayConsumer(consumer);
        }

        public static BodyHandler<byte[]> ofByteArray() {
            return (info) -> BodySubscribers.ofByteArray();
        }

        public static BodyHandler<String> ofString() {
            return (info) -> BodySubscribers.ofString(charsetFrom(info.headers()));
        }

        public static BodyHandler<Flow.Publisher<List<ByteBuffer>>> ofPublisher() {
            return (info) -> BodySubscribers.ofPublisher();
        }

        public static <T> BodyHandler<T> buffering(BodyHandler<T> downstream, int bufferSize) {
            Objects.requireNonNull(downstream);
            return (info) -> BodySubscribers.buffering(downstream.apply(info), bufferSize);
        }

    }

    @Adapter("Ljava/net/http/HttpResponse$BodySubscribers;")
    class BodySubscribers {

        public static BodySubscriber<Void> fromSubscriber(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            return fromSubscriber(subscriber, (s) -> null);
        }

        public static <S extends Flow.Subscriber<? super List<ByteBuffer>>, T> BodySubscriber<Void> fromSubscriber(Flow.Subscriber<? super List<ByteBuffer>> subscriber, Function<? super S,? extends T> finisher) {
            throw MissingStubError.create();
        }

        public static BodySubscriber<Void> fromLineSubscriber(Flow.Subscriber<? super String> subscriber) {
            return fromLineSubscriber(subscriber, (s) -> null, StandardCharsets.UTF_8, null);
        }

        public static BodySubscriber<Void> fromLineSubscriber(
            Flow.Subscriber<? super String> subscriber,
            Function<? super Flow.Subscriber<? super String>,? extends Void> finisher,
            Charset charset,
            String lineSeparator
        ) {
            throw MissingStubError.create();
        }

        public static BodySubscriber<String> ofString(Charset charset) {
            throw MissingStubError.create();
        }

        public static BodySubscriber<byte[]> ofByteArray() {
            throw MissingStubError.create();
        }

        public static BodySubscriber<Path> ofFile(Path file, OpenOption... options) {
            throw MissingStubError.create();
        }

        public static BodySubscriber<Path> ofFile(Path file) {
            return ofFile(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }

        public static BodySubscriber<Void> ofByteArrayConsumer(Consumer<Optional<byte[]>> consumer) {
            throw MissingStubError.create();
        }

        public static BodySubscriber<InputStream> ofInputStream() {
            throw MissingStubError.create();
        }

        public static BodySubscriber<Stream<String>> ofLines(Charset charset) {
            throw MissingStubError.create();
        }

        public static BodySubscriber<Flow.Publisher<List<ByteBuffer>>> ofPublisher() {
            throw MissingStubError.create();
        }

        public static <U> BodySubscriber<U> replacing(U value) {
            throw MissingStubError.create();
        }

        public static BodySubscriber<Void> discarding() {
            throw MissingStubError.create();
        }

        public static <T> BodySubscriber<T> buffering(BodySubscriber<T> downstream, int bufferSize) {
            throw MissingStubError.create();
        }

        public static <T, U> BodySubscriber<U> mapping(BodySubscriber<T> downstream, Function<? super T,? extends U> mapper) {
            throw MissingStubError.create();
        }

    }

    @Adapter("Ljava/net/http/HttpResponse$PushPromiseHandler;")
    interface PushPromiseHandler<T> {

        void applyPushPromise(
            J_N_H_HttpRequest initiatingRequest,
            J_N_H_HttpRequest pushPromiseRequest,
            Function<BodyHandler<T>, CompletableFuture<J_N_H_HttpResponse<T>>> acceptor
        );

        static <T> PushPromiseHandler<T> of(
            Function<J_N_H_HttpRequest, BodyHandler<T>> pushPromiseHandler,
            ConcurrentMap<J_N_H_HttpRequest, CompletableFuture<J_N_H_HttpResponse<T>>> pushPromisesMap
        ) {
            throw MissingStubError.create();
        }

    }


}
