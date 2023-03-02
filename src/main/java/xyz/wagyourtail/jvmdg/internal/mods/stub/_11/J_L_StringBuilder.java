package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_StringBuilder {
    @Stub(JavaVersion.VERSION_11)
    public int compareTo(StringBuilder sb, StringBuilder other) {
        synchronized (sb) {
            return CharSequence.compare(sb, other);
        }
    }
}
