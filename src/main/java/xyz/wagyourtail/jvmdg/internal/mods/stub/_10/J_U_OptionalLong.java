package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class J_U_OptionalLong {

    @Stub(JavaVersion.VERSION_1_10)
    public static long orElseThrow(OptionalLong optional) {
        if (optional.isPresent()) {
            return optional.getAsLong();
        }
        throw new NoSuchElementException("No value present");
    }
}
