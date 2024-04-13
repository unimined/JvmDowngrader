package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.ReverseSet;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;

@Adapter(value = "java/util/SequencedSet", target = "java/util/Set")
public class J_U_SequencedSet<E> {

    static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof J_U_SequencedSet<?> ||
            obj instanceof LinkedHashSet<?> ||
            obj instanceof SortedSet<?>;
    }

    static <E> Set<E> jvmdg$checkcast(Object obj) {
        if (!jvmdg$instanceof(obj)) {
            throw new ClassCastException();
        }
        if (obj instanceof Set<?>) {
            return (Set<E>) obj;
        }
        throw new ClassCastException();
    }

    public static <E> Set<E> reversed(Set<E> self) {
        if (self instanceof ReverseSet<E> rs) {
            return rs.original;
        }
        return new ReverseSet<>(self);
    }

}
