package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class J_U_C_ForkJoinTask {

    @Stub
    public static boolean quietlyJoin(ForkJoinTask<?> fjt, long timeout, TimeUnit unit) throws InterruptedException {
        try {
            fjt.get(timeout, unit);
        } catch (TimeoutException e) {
            return false;
        } catch (ExecutionException ignored) {
        }
        return true;
    }

    @Stub
    public static boolean quietlyJoinUninterruptibly(ForkJoinTask<?> fjt, long timeout, TimeUnit unit) {
        int interruptCount = 0;
        while (true) {
            try {
                fjt.get(timeout, unit);
                break;
            } catch (TimeoutException | ExecutionException ignored) {
                return false;
            } catch (InterruptedException e) {
                ++interruptCount;
                // ignore
            }
        }
        if (interruptCount > 0) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    @Stub(ref = @Ref("Ljava/util/concurrent/ForkJoinTask;"))
    public static <T> ForkJoinTask<T> adaptInterruptible(Callable<? extends T> callable) {
        return ForkJoinTask.adapt(callable);
    }

}
