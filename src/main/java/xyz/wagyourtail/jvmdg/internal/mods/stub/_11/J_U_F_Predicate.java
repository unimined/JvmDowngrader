package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.function.Predicate;

public class J_U_F_Predicate {

    @Stub(value = JavaVersion.VERSION_11, desc = "Ljava/util/function/Predicate;")
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }

}
