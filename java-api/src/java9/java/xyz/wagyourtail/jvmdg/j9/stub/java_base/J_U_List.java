package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.List;

import static xyz.wagyourtail.jvmdg.j9.intl.ImmutableColAccess.*;

public class J_U_List {

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of() {
        return list0();
    }

    @SafeVarargs
    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E... elements) {
        switch (elements.length) { // implicit null check of elements
            case 0:
                return list0();
            case 1:
                return list1(elements[0]);
            case 2:
                return list2(elements[0], elements[1]);
            default:
                return listN(elements);
        }
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1) {
        return list1(e1);
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2) {
        return list2(e1, e2);
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3) {
        return listNTrusted(e1, e2, e3);
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4) {
        return listNTrusted(e1, e2, e3, e4);
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
        return listNTrusted(e1, e2, e3, e4, e5);
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return listNTrusted(e1, e2, e3, e4, e5, e6);
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return listNTrusted(e1, e2, e3, e4, e5, e6, e7);
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return listNTrusted(e1, e2, e3, e4, e5, e6, e7, e8);
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return listNTrusted(e1, e2, e3, e4, e5, e6, e7, e8, e9);
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return listNTrusted(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
    }
}
