package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_IndexOutOfBoundsException {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/IndexOutOfBoundsException;<init>")
    public IndexOutOfBoundsException init(int index) {
        return new IndexOutOfBoundsException("Index out of range: " + index);
    }
}
