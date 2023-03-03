package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.NoSuchElementException;
import java.util.OptionalDouble;

public class J_U_OptionalDouble {

    @Stub(JavaVersion.VERSION_1_10)
    public static double orElseThrow(OptionalDouble optional) {
        if (optional.isPresent()) {
            return optional.getAsDouble();
        }
        throw new NoSuchElementException("No value present");
    }
}
