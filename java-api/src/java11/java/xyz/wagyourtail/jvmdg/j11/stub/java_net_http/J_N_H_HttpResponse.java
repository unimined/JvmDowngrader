package xyz.wagyourtail.jvmdg.j11.stub.java_net_http;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j11.stub.java_base.J_L_String;
import xyz.wagyourtail.jvmdg.util.CharReader;
import xyz.wagyourtail.jvmdg.util.Consumer;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.Adapter;

import javax.net.ssl.SSLSession;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
//            throw MissingStubError.create();
            return StandardCharsets.UTF_8;
        }

        public static BodyHandler<Void> fromSubscriber(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            Objects.requireNonNull(subscriber);
            return (info) -> BodySubscribers.fromSubscriber(subscriber);
        }

        public static <S extends Flow.Subscriber<? super List<ByteBuffer>>, T> BodyHandler<Void> fromSubscriber(Flow.Subscriber<? super List<ByteBuffer>> subscriber, Function<? super S,? extends T> finisher) {
            Objects.requireNonNull(subscriber);
            Objects.requireNonNull(finisher);
            return (info) -> (BodySubscriber<Void>) BodySubscribers.fromSubscriber(subscriber, finisher);
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

        private static Map<String, String> headerContents(String input) {
            CharReader reader = new CharReader(input, 0);
            Map<String, String> headers = new HashMap<>();
            while (!reader.exhausted()) {
                String key;

                if (Character.isWhitespace((char)reader.peek())) {
                    reader.takeWhitespace();
                }

                if (reader.peek() == '"') {
                    key = reader.takeString();

                    if (Character.isWhitespace((char)reader.peek())) {
                        reader.takeWhitespace();
                    }

                } else {
                    key = reader.takeUntil(Set.of((int) ';', (int) '=')).trim();
                }

                if (reader.peek() == '=') {
                    reader.take();

                    String value;

                    if (Character.isWhitespace((char)reader.peek())) {
                        reader.takeWhitespace();
                    }

                    if (reader.peek() == '"') {
                        value = reader.takeString();

                        if (Character.isWhitespace((char)reader.peek())) {
                            reader.takeWhitespace();
                        }
                    } else {
                        value = reader.takeUntil(Set.of((int) ';')).trim();
                    }

                    headers.put(key, value);
                } else {
                    headers.put(key, null);
                }

            }
            return headers;
        }

        public static BodyHandler<Path> ofFileDownload(Path dir, OpenOption... options) {
            Objects.requireNonNull(dir);
            return (info) -> {
                Map<String, String> content = headerContents(info.headers().firstValue("Content-Disposition").orElseThrow());
                return BodySubscribers.ofFile(dir.resolve(content.get("filename")), options);
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

        public static <S extends Flow.Subscriber<? super List<ByteBuffer>>, T> BodySubscriber<T> fromSubscriber(Flow.Subscriber<? super List<ByteBuffer>> subscriber, Function<? super S,? extends T> finisher) {
            CompletableFuture<T> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    subscriber.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                    result.complete(finisher.apply((S)subscriber));
                }

                @Override
                public CompletionStage<T> getBody() {
                    return result;
                }
            };
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
            CompletableFuture<Void> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    subscriber.onNext(item.stream().map((b) -> new String(b.array(), charset)).reduce("", String::concat));
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                    result.complete(finisher.apply(subscriber));
                }

                @Override
                public CompletionStage<Void> getBody() {
                    return result;
                }
            };
        }

        public static BodySubscriber<String> ofString(Charset charset) {
            CompletableFuture<String> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                final StringBuilder builder = new StringBuilder();

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    item.forEach((b) -> builder.append(new String(b.array(), charset)));
                }

                @Override
                public void onError(Throwable throwable) {
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    result.complete(builder.toString());
                }

                @Override
                public CompletionStage<String> getBody() {
                    return result;
                }
            };
        }

        public static BodySubscriber<byte[]> ofByteArray() {
            CompletableFuture<byte[]> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    item.forEach((b) -> {
                        try {
                            out.write(b.array());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    result.complete(out.toByteArray());
                }

                @Override
                public CompletionStage<byte[]> getBody() {
                    return result;
                }
            };
        }

        public static BodySubscriber<Path> ofFile(Path file, OpenOption... options) {
            CompletableFuture<Path> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    try {
                        try (var out = Files.newOutputStream(file, options)) {
                            for (ByteBuffer b : item) {
                                out.write(b.array());
                            }
                        }
                    } catch (IOException e) {
                        result.completeExceptionally(e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    result.complete(file);
                }

                @Override
                public CompletionStage<Path> getBody() {
                    return result;
                }
            };
        }

        public static BodySubscriber<Path> ofFile(Path file) {
            return ofFile(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }

        public static BodySubscriber<Void> ofByteArrayConsumer(Consumer<Optional<byte[]>> consumer) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    item.forEach((b) -> {
                        try {
                            out.write(b.array());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    consumer.accept(Optional.of(out.toByteArray()));
                }

                @Override
                public void onError(Throwable throwable) {
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    consumer.accept(Optional.empty());
                    result.complete(null);
                }

                @Override
                public CompletionStage<Void> getBody() {
                    return result;
                }
            };
        }

        public static BodySubscriber<InputStream> ofInputStream() {
            CompletableFuture<InputStream> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    item.forEach((b) -> {
                        try {
                            out.write(b.array());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    result.complete(new ByteArrayInputStream(out.toByteArray()));
                }

                @Override
                public CompletionStage<InputStream> getBody() {
                    return result;
                }
            };
        }

        public static BodySubscriber<Stream<String>> ofLines(Charset charset) {
            CompletableFuture<Stream<String>> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                final StringBuilder builder = new StringBuilder();

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    item.forEach((b) -> builder.append(new String(b.array(), charset)));
                }

                @Override
                public void onError(Throwable throwable) {
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    result.complete(J_L_String.lines(builder.toString()));
                }

                @Override
                public CompletionStage<Stream<String>> getBody() {
                    return result;
                }
            };
        }

        public static BodySubscriber<Flow.Publisher<List<ByteBuffer>>> ofPublisher() {
            CompletableFuture<Flow.Publisher<List<ByteBuffer>>> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                List<ByteBuffer> buffers = new ArrayList<>();

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    buffers.addAll(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    result.complete(subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
                        @Override
                        public void request(long n) {
                            subscriber.onNext(buffers);
                            subscriber.onComplete();
                        }

                        @Override
                        public void cancel() {
                            subscriber.onComplete();
                        }
                    }));
                }

                @Override
                public CompletionStage<Flow.Publisher<List<ByteBuffer>>> getBody() {
                    return result;
                }

            };
        }

        public static <U> BodySubscriber<U> replacing(U value) {
            CompletableFuture<U> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                }

                @Override
                public void onError(Throwable throwable) {
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    result.complete(value);
                }

                @Override
                public CompletionStage<U> getBody() {
                    return result;
                }
            };
        }

        public static BodySubscriber<Void> discarding() {
            CompletableFuture<Void> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                }

                @Override
                public void onError(Throwable throwable) {
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    result.complete(null);
                }

                @Override
                public CompletionStage<Void> getBody() {
                    return result;
                }
            };
        }

        public static <T> BodySubscriber<T> buffering(BodySubscriber<T> downstream, int bufferSize) {
            CompletableFuture<T> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                List<ByteBuffer> buffers = new ArrayList<>();

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    downstream.onSubscribe(subscription);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    buffers.addAll(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    downstream.onError(throwable);
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    downstream.onComplete();
                    result.complete(downstream.getBody().toCompletableFuture().join());
                }

                @Override
                public CompletionStage<T> getBody() {
                    return result;
                }
            };
        }

        public static <T, U> BodySubscriber<U> mapping(BodySubscriber<T> downstream, Function<? super T,? extends U> mapper) {
            CompletableFuture<U> result = new CompletableFuture<>();
            return new BodySubscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    downstream.onSubscribe(subscription);
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    downstream.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    downstream.onError(throwable);
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    downstream.onComplete();
                    result.complete(mapper.apply(downstream.getBody().toCompletableFuture().join()));
                }

                @Override
                public CompletionStage<U> getBody() {
                    return result;
                }
            };
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
