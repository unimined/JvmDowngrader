package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Collection;
import java.util.Set;

public class J_U_Set {

    @Stub(value = JavaVersion.VERSION_1_10, desc = "Ljava/util/Set;")
    public static <E> Set<E> copyOf(Collection<? extends E> coll) {
        return (Set) Set.of(coll.toArray());
    }

}
