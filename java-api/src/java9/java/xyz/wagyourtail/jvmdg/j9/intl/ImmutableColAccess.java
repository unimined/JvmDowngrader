package xyz.wagyourtail.jvmdg.j9.intl;

import java.util.Collection;
import java.util.List;

import static xyz.wagyourtail.jvmdg.j9.intl.ImmutableCollections.*;

/**
 * bridge class between {@link ImmutableCollections} and {@link List} / {@link java.util.Set} / {@link java.util.Map}
 * static factory method so that modifications to {@link ImmutableCollections} can be minimal
 * @author ZZZank
 */
public interface ImmutableColAccess {
    static <E> List<E> list0() {
        return (List<E>) EMPTY_LIST;
    }

    static <E> List<E> list1(E e1) {
        return new List12<>(e1);
    }

    static <E> List<E> list2(E e1, E e2) {
        return new List12<>(e1, e2);
    }

    static <E> List<E> listN(E... elements) {
        return listFromArray(elements);
    }

    static <E> List<E> listNTrusted(Object... elements) {
        return listFromTrustedArray(elements);
    }

    static <E> List<E> listNTrustedNullable(Object... elements) {
        return listFromTrustedArrayNullsAllowed(elements);
    }

    static <E> List<E> listNCopy(Collection<? extends E> coll) {
        return listCopy(coll);
    }
}
