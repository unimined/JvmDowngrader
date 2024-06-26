package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.j9.intl.IteratorSupport;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.ConcurrentModificationException;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class J_U_R_Matcher {

    @Stub
    public static Matcher appendReplacement(Matcher m, StringBuilder sb, String replacement) {
        StringBuffer buffer = new StringBuffer();
        m.appendReplacement(buffer, replacement);
        sb.append(buffer);
        return m;
    }

    @Stub
    public static StringBuilder appendTail(Matcher m, StringBuilder sb) {
        StringBuffer buffer = new StringBuffer();
        m.appendTail(buffer);
        sb.append(buffer);
        return sb;
    }

    @Stub
    public static String replaceAll(Matcher m, Function<MatchResult, String> replacer) {
        StringBuffer buffer = new StringBuffer();
        while (m.find()) {
            int start = m.start();
            String replacement = replacer.apply(m.toMatchResult());
            if (m.start() != start) {
                throw new ConcurrentModificationException();
            }
            m.appendReplacement(buffer, replacement);
        }
        m.appendTail(buffer);
        return buffer.toString();
    }

    @Stub
    public static Stream<MatchResult> results(Matcher m) {
        return new IteratorSupport<>(m::find, m::toMatchResult).stream();
    }

    @Stub
    public static String replaceFirst(Matcher m, Function<MatchResult, String> replacer) {
        StringBuffer buffer = new StringBuffer();
        if (m.find()) {
            int start = m.start();
            String replacement = replacer.apply(m.toMatchResult());
            if (m.start() != start) {
                throw new ConcurrentModificationException();
            }
            m.appendReplacement(buffer, replacement);
        }
        m.appendTail(buffer);
        return buffer.toString();
    }

}
