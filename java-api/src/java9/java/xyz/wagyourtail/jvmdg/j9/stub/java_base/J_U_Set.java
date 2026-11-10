package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@JEP(269)
public class J_U_Set {

    private static <E> Set<E> setOf(E... elements) {
        Set<E> set = new HashSet<>(elements.length * 2);
        for (E e : elements) {
            if (!set.add(Objects.requireNonNull(e))) {
                throw new IllegalArgumentException("duplicate element: " + e);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of() {
        return Collections.emptySet();
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1) {
        return Collections.singleton(Objects.requireNonNull(e1));
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2) {
        return setOf(e1, e2);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3) {
        return setOf(e1, e2, e3);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4) {
        return setOf(e1, e2, e3, e4);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
        return setOf(e1, e2, e3, e4, e5);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return setOf(e1, e2, e3, e4, e5, e6);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return setOf(e1, e2, e3, e4, e5, e6, e7);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return setOf(e1, e2, e3, e4, e5, e6, e7, e8);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return setOf(e1, e2, e3, e4, e5, e6, e7, e8, e9);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return setOf(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
    }

    @SafeVarargs
    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E... elements) {
        if (elements.length == 0) {
            return of();
        }
        return setOf(elements);
    }


}
