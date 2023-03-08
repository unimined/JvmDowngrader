package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.*;

public class J_U_Set {

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of() {
        return Collections.emptySet();
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1) {
        return Collections.singleton(e1);
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(e1, e2)));
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(e1, e2, e3)));
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(e1, e2, e3, e4)));
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(e1, e2, e3, e4, e5)));
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7)));
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8)));
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9)));
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)));
    }

    @SafeVarargs
    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> of(E... elements) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(elements)));
    }


}
