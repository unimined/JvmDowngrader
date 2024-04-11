package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class J_U_C_ExecutorService {

    @Stub
    public static void close(ExecutorService executor) {
        boolean terminated = executor.isTerminated();
        if (!terminated) {
            executor.shutdown();
            boolean interrupted = false;
            while (!terminated) {
                try {
                    terminated = executor.awaitTermination(1L, TimeUnit.DAYS);
                } catch (InterruptedException e) {
                    if (!interrupted) {
                        executor.shutdownNow();
                        interrupted = true;
                    }
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
