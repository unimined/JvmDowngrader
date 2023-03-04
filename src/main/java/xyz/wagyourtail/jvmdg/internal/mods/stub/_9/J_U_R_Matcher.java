package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.IteratorSupport;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class J_U_R_Matcher {

    @Stub(JavaVersion.VERSION_1_9)
    public static Matcher appendReplacement(Matcher m, StringBuilder sb, String replacement) {
//        MatchResult match;
//        try {
//            match = m.toMatchResult();
//        } catch (NullPointerException e) {
//            throw new IllegalStateException("No match available");
//        }
//        if (match.start() < 0) {
//            throw new IllegalStateException("No match available");
//        }
//        var result = new StringBuilder();
//        int cursor = 0;
//        Pattern expandPattern = Pattern.compile("\\$(?=(\\d+)|\\{([^}*])\\})");
//        Matcher matcher = expandPattern.matcher(replacement);
//        StringBuilder replacementBuilder = new StringBuilder();
//        while (matcher.find()) {
//            replacementBuilder.append(replacement, cursor, matcher.start());
//            String group = matcher.group(1);
//            if (group != null) {
//                int gid = Integer.parseInt(group);
//                StringBuilder sb2 = new StringBuilder();
//                while (gid > match.groupCount() && gid > 9) {
//                    sb2.append(gid % 10);
//                    gid /= 10;
//                }
//                replacementBuilder.append(m.group(Integer.parseInt(group)));
//                replacementBuilder.append(sb2.reverse());
//            } else {
//                if (matcher.group(2).length() == 0) {
//                    throw new IllegalArgumentException("named capturing group has 0 length name");
//                }
//                if (Character.isDigit(matcher.group(2).charAt(0))) {
//                    throw new IllegalArgumentException("capturing group name {" + matcher.group(2) + "} starts with digit character");
//                }
//                replacementBuilder.append(m.group(matcher.group(2)));
//            }
//            cursor = matcher.end();
//        }
//        replacementBuilder.append(replacement, cursor, replacement.length());
//        replacement = replacementBuilder.toString();
//        sb.append(replacement);
        var buffer = new StringBuffer();
        m.appendReplacement(buffer, replacement);
        sb.append(buffer);
        return m;
    }

    @Stub(JavaVersion.VERSION_1_9)
    public static StringBuilder appendTail(Matcher m, StringBuilder sb) {
        var buffer = new StringBuffer();
        m.appendTail(buffer);
        sb.append(buffer);
        return sb;
    }

    @Stub(JavaVersion.VERSION_1_9)
    public static String replaceAll(Matcher m, Function<MatchResult, String> replacer) {
        var buffer = new StringBuffer();
        while (m.find()) {
            int start = m.start();
            var replacement = replacer.apply(m.toMatchResult());
            if (m.start() != start) {
                throw new ConcurrentModificationException();
            }
            m.appendReplacement(buffer, replacement);
        }
        m.appendTail(buffer);
        return buffer.toString();
    }

    @Stub(value = JavaVersion.VERSION_1_9, include = IteratorSupport.class)
    public static Stream<MatchResult> results(Matcher m) {
        return new IteratorSupport<>(m::find, m::toMatchResult).stream();
    }

    @Stub(value = JavaVersion.VERSION_1_9)
    public static String replaceFirst(Matcher m, Function<MatchResult, String> replacer) {
        var buffer = new StringBuffer();
        if (m.find()) {
            int start = m.start();
            var replacement = replacer.apply(m.toMatchResult());
            if (m.start() != start) {
                throw new ConcurrentModificationException();
            }
            m.appendReplacement(buffer, replacement);
        }
        m.appendTail(buffer);
        return buffer.toString();
    }

}
