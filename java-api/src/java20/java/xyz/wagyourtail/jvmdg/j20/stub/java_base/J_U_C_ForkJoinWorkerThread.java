package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ForkJoinWorkerThread;

public class J_U_C_ForkJoinWorkerThread {

    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final Class<?> workQueue;
    private static final MethodHandle getWorkQueue;
    private static final MethodHandle queueSize;

    static {
        try {
            workQueue = Class.forName("java.util.concurrent.ForkJoinPool.WorkQueue");
            getWorkQueue = IMPL_LOOKUP.findGetter(ForkJoinWorkerThread.class, "workQueue", workQueue);
            queueSize = IMPL_LOOKUP.findVirtual(workQueue, "queueSize", MethodType.methodType(int.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Stub
    public static int getQueuedTaskCount(ForkJoinWorkerThread self) throws Throwable {
        return (int) queueSize.invoke(getWorkQueue.invoke(self));
    }

}
