package xyz.wagyourtail.jvmdg.j11.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class J_U_R_Pattern {

    @Stub
    public static Predicate<String> asMatchPredicate(Pattern pattern) {
        return s -> pattern.matcher(s).matches();
    }

}
