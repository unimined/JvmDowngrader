package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class J_L_Process {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();

    private static final MethodHandle getPid;

    static {
        try {
            if (J_L_ProcessHandle.isUnix()) {
                MethodHandle gp;
                try {
                    gp = IMPL_LOOKUP.findGetter(Class.forName("java.lang.UNIXProcess"), "pid", int.class).asType(MethodType.methodType(long.class, Process.class));
                } catch (ClassNotFoundException e) {
                    // we are probably on java 9+
                    gp = IMPL_LOOKUP.findVirtual(Process.class, "pid", MethodType.methodType(long.class));
                }
                getPid = gp;
            } else {
                getPid = IMPL_LOOKUP.findGetter(Class.forName("java.lang.ProcessImpl"), "handle", long.class).asType(MethodType.methodType(long.class, Process.class));
            }
        } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Stub
    public static long pid(Process process) throws Throwable {
        return (long) getPid.invokeExact(process);
    }


    public static CompletableFuture<Process> onExit(Process process) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return process;
        });
    }

    @Stub
    public static J_L_ProcessHandle toHandle(Process process) throws Throwable {
        return J_L_ProcessHandle.of(pid(process)).get();
    }

    @Stub
    public static J_L_ProcessHandle.Info info(Process process) throws Throwable {
        return toHandle(process).info();
    }

    @Stub
    public static Stream<J_L_ProcessHandle> children(Process process) throws Throwable {
        return toHandle(process).children();
    }

    @Stub
    public static Stream<J_L_ProcessHandle> descendants(Process process) throws Throwable {
        return toHandle(process).descendants();
    }

}
