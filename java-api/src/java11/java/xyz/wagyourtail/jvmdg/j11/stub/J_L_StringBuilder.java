package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_StringBuilder {
    @Stub(opcVers = Opcodes.V11)
    public static int compareTo(StringBuilder sb, StringBuilder other) {
        synchronized (sb) {
            return J_L_CharSequence.compare(sb, other);
        }
    }

}
