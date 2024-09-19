package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

@Adapter("java/util/concurrent/Flow")
public class J_U_C_Flow {

    private J_U_C_Flow() {
    }

    public static int defaultBufferSize() {
        return 256;
    }

    @Adapter("java/util/concurrent/Flow$Publisher")
    @FunctionalInterface
    public interface Publisher<T> {
        void subscribe(Subscriber<? super T> subscriber);

    }

    @Adapter("java/util/concurrent/Flow$Subscriber")
    public interface Subscriber<T> {
        void onSubscribe(Subscription subscription);

        void onNext(T item);

        void onError(Throwable throwable);

        void onComplete();

    }

    @Adapter("java/util/concurrent/Flow$Subscription")
    public interface Subscription {
        void request(long n);

        void cancel();

    }

    @Adapter("java/util/concurrent/Flow$Processor")
    public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
    }

}
