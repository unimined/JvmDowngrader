package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.Duration;

public class J_L_Thread {

    @Stub
    public static void join(Thread thread, Duration duration) throws InterruptedException {
        long millis = duration.toMillis();
        int nanos = duration.getNano() % 1_000_000;
        thread.join(millis, nanos);
    }

    @Stub(ref = @Ref("Ljava/lang/Thread;"))
    public static void sleep(Duration duration) throws InterruptedException {
        long millis = duration.toMillis();
        int nanos = duration.getNano() % 1_000_000;
        Thread.sleep(millis, nanos);
    }

    @Stub
    public static long threadId(Thread thread) {
        return thread.getId();
    }

}
