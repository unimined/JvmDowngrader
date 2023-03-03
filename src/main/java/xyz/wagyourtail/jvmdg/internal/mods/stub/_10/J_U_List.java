package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Collection;
import java.util.List;

public class J_U_List {

    @Stub(value = JavaVersion.VERSION_1_10, desc = "Ljava/util/List;")
    public static <E> List<E> copyOf(Collection<? extends E> coll) {
        return (List) List.of(coll.toArray());
    }
}
