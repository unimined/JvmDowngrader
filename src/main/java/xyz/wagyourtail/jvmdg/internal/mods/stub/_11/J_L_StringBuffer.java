package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_StringBuffer {

    @Stub(JavaVersion.VERSION_11)
    public static int compareTo(StringBuffer sb, StringBuffer other) {
        synchronized (sb) {
            return CharSequence.compare(sb, other);
        }
    }
}
