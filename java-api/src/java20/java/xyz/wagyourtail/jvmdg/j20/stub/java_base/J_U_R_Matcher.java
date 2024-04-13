package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class J_U_R_Matcher {

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
        var impl_lookup = Utils.getImplLookup();
        return (Map<String, Integer>) impl_lookup.findVirtual(Pattern.class, "namedGroups", MethodType.methodType(Map.class)).invokeExact(matchResult.pattern());
    }

}
