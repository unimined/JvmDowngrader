package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_StringBuilder {
    @Stub
    public static int compareTo(StringBuilder sb, StringBuilder other) {
        synchronized (sb) {
            return J_L_CharSequence.compare(sb, other);
        }
    }

}
