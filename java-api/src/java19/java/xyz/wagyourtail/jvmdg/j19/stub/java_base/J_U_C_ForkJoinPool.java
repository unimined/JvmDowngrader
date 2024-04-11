package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class J_U_C_ForkJoinPool {

    @Stub
    public static void close(ForkJoinPool fjp) {
        if (fjp != ForkJoinPool.commonPool()) {
            J_U_C_ExecutorService.close(fjp);
        }
    }

    @Stub
    public static <V> ForkJoinTask<V> lazySubmit(ForkJoinPool fjp, ForkJoinTask<V> task) {
        return fjp.submit(task); // this will cause the task to be executed immediately if the pool is empty, which is not lazy
    }

    @Stub
    public static int setParallelism(ForkJoinPool fjp, int parallelism) {
        // this doesn't actually set the parallelism, but it's the best we can do, really
        return fjp.getParallelism();
    }

}
