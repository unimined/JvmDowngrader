package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandles;
import java.util.stream.Stream;

public class J_L_Process {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();

    @Stub
    public static J_L_ProcessHandle toHandle(Process process) throws Throwable {
        long pid = (long) IMPL_LOOKUP.findGetter(process.getClass(), "pid", int.class).invoke(process);
        return J_L_ProcessHandle.of(pid).get();
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
