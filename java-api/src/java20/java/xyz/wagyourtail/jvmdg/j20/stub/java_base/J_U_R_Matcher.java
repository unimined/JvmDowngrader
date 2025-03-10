package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class J_U_R_Matcher {

    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle namedGroups;

    static {
        try {
            namedGroups = IMPL_LOOKUP.findVirtual(Pattern.class, "namedGroups", MethodType.methodType(Map.class));
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    @Stub
    public static boolean hasMatch(Matcher matchResult) {
        try {
            matchResult.start();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Stub
    public static Map<String, Integer> namedGroups(Matcher matchResult) throws Throwable {
        return (Map<String, Integer>) namedGroups.invokeExact(matchResult.pattern());
    }

}
