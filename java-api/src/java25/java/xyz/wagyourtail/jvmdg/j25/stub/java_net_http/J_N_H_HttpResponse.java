package xyz.wagyourtail.jvmdg.j25.stub.java_net_http;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class J_N_H_HttpResponse {

    @Stub
    public static Optional<String> connectionLabel(HttpResponse<?> response) {
        return Optional.empty();
    }

    public static class BodyHandlers {

        @Stub(ref = @Ref("java/net/http/HttpResponse$BodyHandlers"))
        public static <T> HttpResponse.BodyHandler<T> limiting(HttpResponse.BodyHandler<T> downstreamHandler, long capacity) {
            Objects.requireNonNull(downstreamHandler, "downstreamHandler");
            if (capacity < 0) {
                throw new IllegalArgumentException("capacity must not be negative: " + capacity);
            }
            return info -> {
                HttpResponse.BodySubscriber<T> downstreamSubscriber = downstreamHandler.apply(info);
                return BodySubscribers.limiting(downstreamSubscriber, capacity);
            };
        }

    }

    public static class BodySubscribers {

        @Stub(ref = @Ref("java/net/http/HttpResponse$BodySubscribers"))
        public static <T> HttpResponse.BodySubscriber<T> limiting(HttpResponse.BodySubscriber<T> downstreamSubscriber, long capacity) {
            Objects.requireNonNull(downstreamSubscriber, "downstreamSubscriber");
            if (capacity < 0) {
                throw new IllegalArgumentException("capacity must not be negative: " + capacity);
            }
            return new HttpResponse.BodySubscriber<>() {
                long remainingCapacity = capacity;
                Flow.Subscription subscription;
                boolean completed;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    Objects.requireNonNull(subscription, "subscription");
                    if (completed || this.subscription != null) {
                        subscription.cancel();
                    } else {
                        this.subscription = subscription;
                        downstreamSubscriber.onSubscribe(subscription);
                    }
                }

                @Override
                public void onNext(List<ByteBuffer> item) {
                    Objects.requireNonNull(item, "item");

                    if (subscription == null) {
                        return;
                    }

                    try {
                        long size = item.stream().mapToLong(ByteBuffer::remaining).reduce(0, Math::addExact);
                        if (size > remainingCapacity) throw new ArithmeticException();
                        remainingCapacity -= size;
                        downstreamSubscriber.onNext(item);
                    } catch (ArithmeticException _) {
                        completed = true;
                        subscription = null;
                        downstreamSubscriber.onError(new IOException("body exceeded capacity: " + capacity));
                        subscription.cancel();
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    Objects.requireNonNull(throwable, "throwable");
                    if (subscription != null) {
                        subscription = null;
                        completed = true;
                        downstreamSubscriber.onError(throwable);
                    }
                }

                @Override
                public void onComplete() {
                    if (subscription != null) {
                        subscription = null;
                        completed = true;
                        downstreamSubscriber.onComplete();
                    }
                }

                @Override
                public CompletionStage<T> getBody() {
                    return downstreamSubscriber.getBody();
                }
            };
        }

    }

}
