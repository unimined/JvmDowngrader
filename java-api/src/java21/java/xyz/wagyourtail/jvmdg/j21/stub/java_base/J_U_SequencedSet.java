package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.SequencedSetAdapter;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.*;

@Adapter("java/util/SequencedSet")
public interface J_U_SequencedSet<E> extends J_U_SequencedCollection<E>, Set<E> {

    @Override
    J_U_SequencedSet<E> reversed();

    static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof J_U_SequencedSet<?> ||
            obj instanceof LinkedHashSet<?> ||
            obj instanceof SortedSet<?>;
    }

    static <E> J_U_SequencedSet<E> jvmdg$checkcast(Object obj) {
        if (!jvmdg$instanceof(obj)) {
            throw new ClassCastException();
        }
        if (obj instanceof J_U_SequencedSet<?>) {
            return (J_U_SequencedSet<E>) obj;
        }
        if (obj instanceof Set<?>) {
            return new SequencedSetAdapter<>((Set<E>) obj);
        }
        throw new ClassCastException();
    }

}
