package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class J_U_C_ForkJoinPool {

    @Stub
    public static <V> ForkJoinTask<V> externalSubmit(ForkJoinPool self, ForkJoinTask<V> task) {
        return self.submit(task); // TODO: submit from another thread pool.
    }

}
