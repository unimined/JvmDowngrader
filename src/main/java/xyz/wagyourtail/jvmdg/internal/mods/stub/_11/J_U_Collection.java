package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Collection;
import java.util.function.IntFunction;

public class J_U_Collection {

    @Stub(value = JavaVersion.VERSION_11, subtypes = true)
    public static <T> T[] toArray(Collection<T> collection, IntFunction<T[]> generator) {
        return collection.toArray(generator.apply(0));
    }

}
