package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.NoSuchElementException;
import java.util.Optional;

public class J_U_Optional {

    @Stub(JavaVersion.VERSION_1_10)
    public static <T> T orElseThrow(Optional<T> optional) {
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NoSuchElementException("No value present");
    }
}
