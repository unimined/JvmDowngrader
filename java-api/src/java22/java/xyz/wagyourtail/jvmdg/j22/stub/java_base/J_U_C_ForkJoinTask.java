package xyz.wagyourtail.jvmdg.j22.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.ForkJoinTask;

public class J_U_C_ForkJoinTask {

    @Stub(ref = @Ref("Ljava/util/concurrent/ForkJoinTask;"))
    public static ForkJoinTask<?> adaptInterruptible(Runnable runnable) {
        return ForkJoinTask.adapt(runnable);
    }

}
