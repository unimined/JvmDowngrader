package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.ThreadPerTaskExecutor;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public class J_U_C_Executors {

    @Stub(ref = @Ref(value = "Ljava/util/concurrent/Executors;", member = "newVirtualThreadPerTaskExecutor",
        desc = "()Ljava/util/concurrent/ExecutorService;"))
    public static ExecutorService newVirtualThreadPerTaskExecutor() {
        return new ThreadPerTaskExecutor(J_L_Thread.ofVirtual().factory());
    }

    @Stub(ref = @Ref(value = "Ljava/util/concurrent/Executors;", member = "newThreadPerTaskExecutor",
        desc = "(Ljava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;"))
    public static ExecutorService newThreadPerTaskExecutor(ThreadFactory factory) {
        return new ThreadPerTaskExecutor(factory);
    }

}
