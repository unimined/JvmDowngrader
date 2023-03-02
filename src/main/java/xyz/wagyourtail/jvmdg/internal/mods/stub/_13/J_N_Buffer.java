package xyz.wagyourtail.jvmdg.internal.mods.stub._13;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.nio.*;

public class J_N_Buffer {


    @Stub(value = JavaVersion.VERSION_13, subtypes = true, returnDecendant = true)
    public static Buffer slice(Buffer buffer, int index, int length) {
        int pos = buffer.position();
        buffer.position(index);
        var sub = buffer.slice().limit(length);
        buffer.position(pos);
        return sub;
    }
}
