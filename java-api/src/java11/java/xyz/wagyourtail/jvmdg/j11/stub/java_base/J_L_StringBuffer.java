package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_StringBuffer {

    @Stub
    public static int compareTo(StringBuffer sb, StringBuffer other) {
        synchronized (sb) {
            return J_L_CharSequence.compare(sb, other);
        }
    }

}
