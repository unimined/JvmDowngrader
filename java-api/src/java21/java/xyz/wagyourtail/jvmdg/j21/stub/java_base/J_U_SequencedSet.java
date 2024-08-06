package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.ReverseSet;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;

@Adapter(value = "java/util/SequencedSet", target = "java/util/Set")
public class J_U_SequencedSet {

    private J_U_SequencedSet() {
    }

    public static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof LinkedHashSet<?> ||
                obj instanceof SortedSet<?> ||
                obj instanceof ReverseSet<?>;
    }

    public static <E> Set<E> jvmdg$checkcast(Object obj) {
        if (!jvmdg$instanceof(obj)) {
            throw new ClassCastException();
        }
        if (obj instanceof Set<?>) {
            return (Set<E>) obj;
        }
        throw new ClassCastException();
    }

    @Stub
    public static <E> Set<E> reversed(Set<E> self) {
        if (self instanceof ReverseSet<E> rs) {
            return rs.original;
        }
        return new ReverseSet<>(self);
    }

}
