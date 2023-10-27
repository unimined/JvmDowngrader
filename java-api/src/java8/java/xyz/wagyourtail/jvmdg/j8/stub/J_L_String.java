package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_String {

    @Stub(ref = @Ref("java/lang/String"))
    public static String join(CharSequence delimiter, CharSequence... elements) {
        StringBuilder sb = new StringBuilder(delimiter);
        for (CharSequence element : elements) {
            sb.append(element);
        }
        return sb.toString();
    }

    @Stub(ref = @Ref("java/lang/String"))
    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        StringBuilder sb = new StringBuilder(delimiter);
        for (CharSequence element : elements) {
            sb.append(element);
        }
        return sb.toString();
    }

}
