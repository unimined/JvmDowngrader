package xyz.wagyourtail.jvmdg.internal.mods.stub._13;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.nio.Buffer;

public class J_N_Buffer {


    @Stub(JavaVersion.VERSION_13)
    public static Buffer slice(Buffer buffer, int index, int length) {
        int pos = buffer.position();
        buffer.position(index);
        var sub = buffer.slice().limit(length);
        buffer.position(pos);
        return sub;
    }

    //TODO: buffer subclasses
}
