package xyz.wagyourtail.jvmdg.j22.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.Console;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class J_I_Console {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle ISTTY;

    static {
        try {
            ISTTY = IMPL_LOOKUP.findStatic(Console.class, "istty", MethodType.methodType(boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Stub
    public static boolean isTerminal(Console console) throws Throwable {
        return console.getClass().equals(Console.class) && (boolean) ISTTY.invokeExact();
    }

}
