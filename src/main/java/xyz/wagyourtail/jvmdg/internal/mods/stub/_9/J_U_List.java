package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.*;

public class J_U_List {

    @SafeVarargs
    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E ...coll) {
        return Collections.unmodifiableList(Arrays.asList(coll));
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E e1) {
        return Collections.singletonList(e1);
    }
    
    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E e1, E e2) {
        return Collections.unmodifiableList(Arrays.asList(e1, e2));
    }
    
    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E e1, E e2, E e3) {
        return Collections.unmodifiableList(Arrays.asList(e1, e2, e3));
    }
    
    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E e1, E e2, E e3, E e4) {
        return Collections.unmodifiableList(Arrays.asList(e1, e2, e3, e4));
    }
    
    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
        return Collections.unmodifiableList(Arrays.asList(e1, e2, e3, e4, e5));
    }
    
    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return Collections.unmodifiableList(Arrays.asList(e1, e2, e3, e4, e5, e6));
    }
    
    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return Collections.unmodifiableList(Arrays.asList(e1, e2, e3, e4, e5, e6, e7));
    }
    
    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return Collections.unmodifiableList(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8));
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return Collections.unmodifiableList(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9));
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/List;")
    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return Collections.unmodifiableList(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
    }
}
