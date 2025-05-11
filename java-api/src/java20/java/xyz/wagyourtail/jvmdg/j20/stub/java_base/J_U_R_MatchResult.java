package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.exc.PartialStubError;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class J_U_R_MatchResult {

    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle namedGroups;

    static {
        try {
            namedGroups = IMPL_LOOKUP.findVirtual(Pattern.class, "namedGroups", MethodType.methodType(Map.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Stub
    public static int end(MatchResult matchResult, String groupName) {
        if (matchResult instanceof Matcher matcher) {
            return matcher.end(groupName);
        } else {
            throw PartialStubError.create();
        }
    }


    @Stub
    public static String group(MatchResult matchResult, String groupName) {
        if (matchResult instanceof Matcher matcher) {
            return matcher.group(groupName);
        } else {
            throw PartialStubError.create();
        }
    }

    @Stub
    public static boolean hasMatch(MatchResult matchResult) {
        try {
            matchResult.start();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Stub
    public static Map<String, Integer> namedGroups(MatchResult matchResult) throws Throwable {
        if (matchResult instanceof Matcher matcher) {
            return (Map<String, Integer>) namedGroups.invokeExact(matcher.pattern());
        } else {
            throw PartialStubError.create();
        }
    }

    @Stub
    public static int start(MatchResult matchResult, String groupName) {
        if (matchResult instanceof Matcher matcher) {
            return matcher.start(groupName);
        } else {
            throw PartialStubError.create();
        }
    }

}
