package xyz.wagyourtail.jvmdg.internal.mods.stub._16;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_IndexOutOfBoindsException {
    @Stub(value = JavaVersion.VERSION_16, desc = "Ljava/lang/IndexOutOfBoundsException;<init>")
    public static IndexOutOfBoundsException create(long i) {
        return new IndexOutOfBoundsException("Index out of range: " + i);
    }
}
