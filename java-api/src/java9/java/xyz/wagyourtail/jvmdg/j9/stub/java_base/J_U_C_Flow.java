package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

@Stub(ref = @Ref("java/util/concurrent/Flow"))
public class J_U_C_Flow {

    @Stub(ref = @Ref("java/util/concurrent/Flow$Publisher"))
    @FunctionalInterface
    public interface Publisher<T> {
        void subscribe(Subscriber<? super T> subscriber);
    }

    @Stub(ref = @Ref("java/util/concurrent/Flow$Subscriber"))
    public interface Subscriber<T> {
        void onSubscribe(Subscription subscription);

        void onNext(T item);

        void onError(Throwable throwable);

        void onComplete();
    }

    @Stub(ref = @Ref("java/util/concurrent/Flow$Subscription"))
    public interface Subscription {
        void request(long n);

        void cancel();
    }

    @Stub(ref = @Ref("java/util/concurrent/Flow$Processor"))
    public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
    }

    public static int defaultBufferSize() {
        return 256;
    }

}
