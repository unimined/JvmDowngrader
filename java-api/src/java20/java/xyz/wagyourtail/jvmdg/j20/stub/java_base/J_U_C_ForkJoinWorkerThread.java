package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodType;
import java.util.concurrent.ForkJoinWorkerThread;

public class J_U_C_ForkJoinWorkerThread {

    @Stub
    public static int getQueuedTaskCount(ForkJoinWorkerThread self) throws Throwable {
        var IMPL_LOOKUP = Utils.getImplLookup();
        var workQueue = Class.forName("java.util.concurrent.ForkJoinPool.WorkQueue");
        var queue = IMPL_LOOKUP.findGetter(ForkJoinWorkerThread.class, "workQueue", workQueue).invoke(self);
        return (int) IMPL_LOOKUP.findVirtual(workQueue, "queueSize", MethodType.methodType(int.class)).invokeExact(queue);
    }

}
