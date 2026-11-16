package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.Duration;

public class J_L_Thread {

    @Stub
    public static boolean join(Thread thread, Duration duration) throws InterruptedException {
        // mirrors Thread.join(Duration) since 19: state checks, then a
        // millis join rounded up so the wait is never shortened
        long nanos = java.util.concurrent.TimeUnit.NANOSECONDS.convert(duration);
        Thread.State state = thread.getState();
        if (state == Thread.State.NEW) {
            throw new IllegalThreadStateException("Thread not started");
        }
        if (state == Thread.State.TERMINATED) {
            return true;
        }
        if (nanos <= 0) {
            return false;
        }
        long millis = java.util.concurrent.TimeUnit.MILLISECONDS.convert(nanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        if (nanos > java.util.concurrent.TimeUnit.NANOSECONDS.convert(millis, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            millis++;
        }
        thread.join(millis);
        return !thread.isAlive();
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
