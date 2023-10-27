package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class J_U_C_ForkJoinPool {
    private static final ForkJoinPool COMMON_POOL;

    static {
        ForkJoinPool.ForkJoinWorkerThreadFactory fac = ForkJoinPool.defaultForkJoinWorkerThreadFactory;
        Thread.UncaughtExceptionHandler handler = null;
        int pc = 0, preset = 0; // nonzero if size set as property
        try {  // ignore exceptions in accessing/parsing properties
            String pp = System.getProperty
                    ("java.util.concurrent.ForkJoinPool.common.parallelism");
            if (pp != null) {
                pc = Math.max(0, Integer.parseInt(pp));
                preset = 1 << 21;
            }
            String sf = System.getProperty
                    ("java.util.concurrent.ForkJoinPool.common.threadFactory");
            String sh = System.getProperty
                    ("java.util.concurrent.ForkJoinPool.common.exceptionHandler");
            if (sf != null || sh != null) {
                ClassLoader ldr = ClassLoader.getSystemClassLoader();
                if (sf != null)
                    fac = (ForkJoinPool.ForkJoinWorkerThreadFactory)
                            ldr.loadClass(sf).getConstructor().newInstance();
                if (sh != null)
                    handler = (Thread.UncaughtExceptionHandler)
                            ldr.loadClass(sh).getConstructor().newInstance();
            }
        } catch (Exception ignore) {
        }
        if (preset == 0)
            pc = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        int p = Math.min(pc, Short.MAX_VALUE);
        COMMON_POOL = new ForkJoinPool(p, fac, handler, true);
    }

    @Stub(ref = @Ref("java/util/concurrent/ForkJoinPool"))
    public static ForkJoinPool commonPool() {
        return COMMON_POOL;
    }

    @Stub(ref = @Ref("java/util/concurrent/ForkJoinPool"))
    public static int getCommonPoolParallelism() {
        return COMMON_POOL.getParallelism();
    }
}
