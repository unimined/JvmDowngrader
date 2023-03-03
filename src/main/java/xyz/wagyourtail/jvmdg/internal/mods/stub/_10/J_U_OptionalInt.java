package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class J_U_OptionalInt {

    @Stub(JavaVersion.VERSION_1_10)
    public static int orElseThrow(OptionalInt optional) {
        if (optional.isPresent()) {
            return optional.getAsInt();
        }
        throw new NoSuchElementException("No value present");
    }
}
