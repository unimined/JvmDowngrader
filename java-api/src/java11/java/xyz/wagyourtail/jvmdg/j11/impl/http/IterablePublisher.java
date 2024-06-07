package xyz.wagyourtail.jvmdg.j11.impl.http;

import java.util.Iterator;
import java.util.concurrent.Flow;

public class IterablePublisher<T> implements Flow.Publisher<T> {
    private final Iterable<T> iterable;
    private final Throwable throwable;

    public IterablePublisher(Iterable<T> iterable, Throwable throwable) {
        this.iterable = iterable;
        this.throwable = throwable;
    }

    public IterablePublisher(Iterable<T> iterable) {
        this(iterable, null);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Subscription(subscriber));
    }

    private class Subscription implements Flow.Subscription {
        private final Flow.Subscriber<? super T> subscriber;
        private final Iterator<T> iterator;
        private volatile boolean completed;
        private volatile boolean cancelled;
        private volatile Throwable error;

        public Subscription(Flow.Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
            this.iterator = iterable.iterator();
            this.error = throwable;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException("n <= 0"));
                return;
            }
            if (completed || cancelled) {
                return;
            }
            if (error != null) {
                completed = true;
                subscriber.onError(error);
                return;
            }
            try {
                synchronized (this) {
                    for (long i = 0; i < n; i++) {
                        if (cancelled) {
                            break;
                        }
                        if (iterator.hasNext()) {
                            subscriber.onNext(iterator.next());
                        } else {
                            completed = true;
                            subscriber.onComplete();
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                error = t;
                subscriber.onError(t);
                completed = true;
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
