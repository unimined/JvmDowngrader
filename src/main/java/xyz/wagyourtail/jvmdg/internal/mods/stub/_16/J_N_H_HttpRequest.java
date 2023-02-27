package xyz.wagyourtail.jvmdg.internal.mods.stub._16;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.function.BiPredicate;

public class J_N_H_HttpRequest {

    @Stub(value = JavaVersion.VERSION_16, desc = "Ljava/net/http/HttpRequest;")
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

    @Stub(value = JavaVersion.VERSION_16, desc = "Ljava/net/http/HttpRequest;", include = {AggregatePublisher.class})
    public static HttpRequest.BodyPublisher concat(HttpRequest.BodyPublisher... publishers) {
        if (publishers.length == 0) {
            return HttpRequest.BodyPublishers.noBody();
        } else if (publishers.length == 1) {
            return Objects.requireNonNull(publishers[0]);
        } else {
            return new AggregatePublisher(publishers);
        }
    }

    public static class AggregatePublisher implements HttpRequest.BodyPublisher {

        private final HttpRequest.BodyPublisher[] publishers;

        public AggregatePublisher(HttpRequest.BodyPublisher... publishers) {
            this.publishers = publishers;
        }


        private static long reduceLength(long a, long b) {
            if (a < 0 || b < 0) {
                return -1;
            } else {
                return a + b;
            }
        }

        @Override
        public long contentLength() {
            long l = Arrays.stream(publishers).mapToLong(HttpRequest.BodyPublisher::contentLength).reduce(AggregatePublisher::reduceLength).orElse(0L);
            if (l < 0) {
                return -1;
            } else {
                return l;
            }
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            throw new UnsupportedOperationException("jvmdowgrader does not support this method (yet).");
        }
    }
}
