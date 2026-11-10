package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import xyz.wagyourtail.jvmdg.j9.intl.ImmutableList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JEP(269)
public class J_U_List {

    private static <E> List<E> checked(List<E> list) {
        for (E e : list) {
            Objects.requireNonNull(e);
        }
        return list;
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of() {
        return checked(new ImmutableList<>(Collections.emptyList()));
    }

    @SafeVarargs
    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E... coll) {
        if (coll.length == 0) {
            return of();
        }
        return checked(new ImmutableList<>(Arrays.asList(coll)));
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1) {
        return checked(new ImmutableList<>(Collections.singletonList(e1)));
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2) {
        return checked(new ImmutableList<>(Arrays.asList(e1, e2)));
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3) {
        return checked(new ImmutableList<>(Arrays.asList(e1, e2, e3)));
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4) {
        return checked(new ImmutableList<>(Arrays.asList(e1, e2, e3, e4)));
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
        return checked(new ImmutableList<>(Arrays.asList(e1, e2, e3, e4, e5)));
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return checked(new ImmutableList<>(Arrays.asList(e1, e2, e3, e4, e5, e6)));
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return checked(new ImmutableList<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7)));
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return checked(new ImmutableList<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8)));
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return checked(new ImmutableList<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9)));
    }

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return checked(new ImmutableList<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)));
    }

}
