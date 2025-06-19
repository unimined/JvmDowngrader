package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Set;

import static xyz.wagyourtail.jvmdg.j9.intl.ImmutableColAccess.*;

public class J_U_Set {

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of() {
        return set0();
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1) {
        return set1(e1);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2) {
        return set2(e1, e2);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3) {
        return setN(e1, e2, e3);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4) {
        return setN(e1, e2, e3, e4);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
        return setN(e1, e2, e3, e4, e5);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return setN(e1, e2, e3, e4, e5, e6);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return setN(e1, e2, e3, e4, e5, e6, e7);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return setN(e1, e2, e3, e4, e5, e6, e7, e8);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return setN(e1, e2, e3, e4, e5, e6, e7, e8, e9);
    }

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return setN(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
    }

    @SafeVarargs
    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E... elements) {
        switch (elements.length) { // implicit null check of elements
            case 0:
                return set0();
            case 1:
                return set1(elements[0]);
            case 2:
                return set2(elements[0], elements[1]);
            default:
                return setN(elements);
        }
    }


}
