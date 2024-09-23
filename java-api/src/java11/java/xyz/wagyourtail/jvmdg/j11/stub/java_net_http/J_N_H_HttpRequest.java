package xyz.wagyourtail.jvmdg.j11.stub.java_net_http;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j11.impl.http.HttpRequestBuilderImpl;
import xyz.wagyourtail.jvmdg.j11.impl.http.IterablePublisher;
import xyz.wagyourtail.jvmdg.j11.impl.http.StreamIterator;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

@Adapter("Ljava/net/http/HttpRequest;")
public abstract class J_N_H_HttpRequest {
    protected J_N_H_HttpRequest() {}

    public static Builder newBuilder() {
        return new HttpRequestBuilderImpl();
    }

    public static Builder newBuilder(URI uri) {
        return new HttpRequestBuilderImpl().uri(uri);
    }

    public abstract Optional<BodyPublisher> bodyPublisher();

    public abstract String method();

    public abstract Optional<Duration> timeout();

    public abstract boolean expectContinue();

    public abstract URI uri();

    public abstract Optional<J_N_H_HttpClient.Version> version();

    public abstract J_N_H_HttpHeaders headers();

    public final boolean equals(Object obj) {
        if (!(obj instanceof J_N_H_HttpRequest)) {
            return false;
        }
        J_N_H_HttpRequest that = (J_N_H_HttpRequest) obj;
        if (!that.method().equals(this.method())) {
            return false;
        }
        if (!that.headers().equals(this.headers())) {
            return false;
        }
        return that.uri().equals(this.uri());
    }

    public final int hashCode() {
        return Objects.hash(method().hashCode(), uri().hashCode(), headers().hashCode());
    }

    @Adapter("Ljava/net/http/HttpRequest$BodyPublisher;")
    public interface BodyPublisher extends Flow.Publisher<ByteBuffer> {
        long contentLength();
    }

    @Adapter("Ljava/net/http/HttpRequest$Builder;")
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

    @Adapter("Ljava/net/http/HttpRequest$BodyPublishers;")
    public static class BodyPublishers {
        private BodyPublishers() {
        }

        public static BodyPublisher fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher) {
            return fromPublisher(publisher, -1);
        }

        public static BodyPublisher fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher, long contentLength) {
            Objects.requireNonNull(publisher);
            return new BodyPublisher() {

                @Override
                public long contentLength() {
                    return contentLength;
                }

                @Override
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                    publisher.subscribe(subscriber);
                }
            };
        }

        public static BodyPublisher ofString(String s) {
            return ofString(s, StandardCharsets.UTF_8);
        }

        public static BodyPublisher ofString(String s, Charset charset) {
            return ofByteArray(s.getBytes(charset));
        }

        public static BodyPublisher ofInputStream(Supplier<? extends InputStream> streamSupplier) {
            return new BodyPublisher() {
                @Override
                public long contentLength() {
                    return -1;
                }

                @Override
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                    IterablePublisher<ByteBuffer> publisher;
                    InputStream is = streamSupplier.get();
                    if (is == null) {
                        publisher = new IterablePublisher<>(null, new NullPointerException("InputStream supplier returned null"));
                    } else {
                        publisher = new IterablePublisher<>(() -> new StreamIterator(is));
                    }
                    publisher.subscribe(subscriber);
                }
            };
        }

        public static BodyPublisher ofByteArray(byte[] bytes) {
            return ofByteArray(bytes, 0, bytes.length);
        }

        public static BodyPublisher ofByteArray(byte[] bytes, int offset, int length) {
            return new BodyPublisher() {
                @Override
                public long contentLength() {
                    return length;
                }

                @Override
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                    subscriber.onSubscribe(new Flow.Subscription() {
                        private boolean completed;

                        @Override
                        public void request(long n) {
                            if (n <= 0) {
                                subscriber.onError(new IllegalArgumentException("n <= 0"));
                                return;
                            }
                            if (completed) {
                                return;
                            }
                            completed = true;
                            subscriber.onNext(ByteBuffer.wrap(bytes, offset, length));
                            subscriber.onComplete();
                        }

                        @Override
                        public void cancel() {
                            completed = true;
                        }
                    });
                }
            };
        }

        public static BodyPublisher ofFile(Path file) {
            return new BodyPublisher() {
                @Override
                public long contentLength() {
                    try {
                        return Files.size(file);
                    } catch (Exception e) {
                        return -1;
                    }
                }

                @Override
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                    subscriber.onSubscribe(new Flow.Subscription() {
                        private boolean completed;

                        @Override
                        public void request(long n) {
                            if (n <= 0) {
                                subscriber.onError(new IllegalArgumentException("n <= 0"));
                                return;
                            }
                            if (completed) {
                                return;
                            }
                            completed = true;
                            try (InputStream is = Files.newInputStream(file)) {
                                subscriber.onNext(ByteBuffer.wrap(is.readAllBytes()));
                                subscriber.onComplete();
                            } catch (Exception e) {
                                subscriber.onError(e);
                            }
                        }

                        @Override
                        public void cancel() {
                            completed = true;
                        }
                    });
                }
            };
        }

        public static BodyPublisher ofByteArrays(Iterable<byte[]> byteArrays) {
            return new BodyPublisher() {
                @Override
                public long contentLength() {
                    return -1;
                }

                @Override
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                    IterablePublisher<ByteBuffer> publisher = new IterablePublisher<>(() -> new Iterator<ByteBuffer>() {
                        final Iterator<byte[]> iterator = byteArrays.iterator();

                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public ByteBuffer next() {
                            return ByteBuffer.wrap(iterator.next());
                        }
                    });
                    publisher.subscribe(subscriber);
                }
            };
        }

        public static BodyPublisher noBody() {
            return new BodyPublisher() {
                @Override
                public long contentLength() {
                    return 0;
                }

                @Override
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                    subscriber.onSubscribe(new Flow.Subscription() {
                        @Override
                        public void request(long n) {
                            subscriber.onComplete();
                        }

                        @Override
                        public void cancel() {
                        }
                    });
                }
            };
        }
    }
}
