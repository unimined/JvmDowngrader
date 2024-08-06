package xyz.wagyourtail.jvmdg.j16.stub.java_net_http;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.function.BiPredicate;

public class J_N_H_HttpRequest {

    @Stub(ref = @Ref("Ljava/net/http/HttpRequest;"))
    public static HttpRequest.Builder newBuilder(HttpRequest request, BiPredicate<String, String> filter) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(filter);

        final HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(request.uri());
        builder.expectContinue(request.expectContinue());

        HttpHeaders headers = HttpHeaders.of(request.headers().map(), filter);
        for (Map.Entry<String, List<String>> entry : headers.map().entrySet()) {
            for (String value : entry.getValue()) {
                builder.header(entry.getKey(), value);
            }
        }

        request.version().ifPresent(builder::version);
        request.timeout().ifPresent(builder::timeout);

        var method = request.method();
        var publisher = request.bodyPublisher();
        if (publisher.isPresent()) {
            builder.method(method, publisher.get());
        } else {
            switch (method) {
                case "GET" -> builder.GET();
                case "DELETE" -> builder.DELETE();
                default -> builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
        }
        return builder;
    }

    public static class BodyPublishers {
    
        @Stub(ref = @Ref("java/net/http/HttpRequest$BodyPublishers"))
        public static HttpRequest.BodyPublisher concat(HttpRequest.BodyPublisher... publishers) {
            return new HttpRequest.BodyPublisher() {
                @Override
                public long contentLength() {
                    long sum = 0;
                    for (HttpRequest.BodyPublisher publisher : publishers) {
                        sum += publisher.contentLength();
                    }
                    return sum;
                }
    
                @Override
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                    subscriber.onSubscribe(new Flow.Subscription() {
                        private final Iterator<HttpRequest.BodyPublisher> iterator = List.of(publishers).iterator();
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
                            try {
                                while (n > 0) {
                                    if (!iterator.hasNext()) {
                                        completed = true;
                                        subscriber.onComplete();
                                        return;
                                    }
                                    HttpRequest.BodyPublisher publisher = iterator.next();
                                    long contentLength = publisher.contentLength();
                                    if (contentLength > 0) {
                                        publisher.subscribe(new Flow.Subscriber<>() {
                                            private long remaining = contentLength;
    
                                            @Override
                                            public void onSubscribe(Flow.Subscription subscription) {
                                                subscription.request(Long.MAX_VALUE);
                                            }
    
                                            @Override
                                            public void onNext(ByteBuffer item) {
                                                subscriber.onNext(item);
                                                remaining -= item.remaining();
                                                if (remaining == 0) {
                                                    subscriber.onComplete();
                                                }
                                            }
    
                                            @Override
                                            public void onError(Throwable throwable) {
                                                subscriber.onError(throwable);
                                            }
    
                                            @Override
                                            public void onComplete() {
                                            }
                                        });
                                    } else {
                                        publisher.subscribe(new Flow.Subscriber<>() {
                                            @Override
                                            public void onSubscribe(Flow.Subscription subscription) {
                                                subscription.request(Long.MAX_VALUE);
                                            }
    
                                            @Override
                                            public void onNext(ByteBuffer item) {
                                                subscriber.onNext(item);
                                            }
    
                                            @Override
                                            public void onError(Throwable throwable) {
                                                subscriber.onError(throwable);
                                            }
    
                                            @Override
                                            public void onComplete() {
                                                subscriber.onComplete();
                                            }
                                        });
                                    }
                                    n--;
                                }
                            } catch (Throwable t) {
                                subscriber.onError(t);
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
    
    }
}
