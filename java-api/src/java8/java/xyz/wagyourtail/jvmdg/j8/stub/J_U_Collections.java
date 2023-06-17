package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.j8.intl.collections.*;
import xyz.wagyourtail.jvmdg.j8.stub.collections.*;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.*;

public class J_U_Collections {

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <T> NavigableSet<T> unmodifiableNavigableSet(NavigableSet<? extends T> s) {
        return (NavigableSet<T>) new UnmodifiableNavigableCollection<>(s);
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(NavigableMap<K, ? extends V> m) {
        return (NavigableMap<K, V>) new UnmodifiableNavigableMap<>(m);
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <T> NavigableSet<T> synchronizedNavigableSet(NavigableSet<T> s) {
        return new SynchronizedNavigableSet<>(s);
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <K, V> NavigableMap<K, V> synchronizedNavigableMap(NavigableMap<K, V> m) {
        return new SynchronizedNavigableMap<>(m);
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <E> Queue<E> checkedQueue(Queue<E> queue, Class<E> type) {
        return new CheckedQueue<>(queue, type);
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <E> NavigableSet<E> checkedNavigableSet(NavigableSet<E> s, Class<E> type) {
        return new CheckedNavigableSet<>(s, type);
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <K, V> NavigableMap<K, V> checkedNavigableMap(NavigableMap<K, V> s, Class<K> keyType, Class<V> valueType) {
        return new CheckedNavigableMap<>(s, keyType, valueType);
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <E> SortedSet<E> emptySortedSet() {
        return Collections.unmodifiableSortedSet(new TreeSet());
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <E> NavigableSet<E> emptyNavigableSet() {
        return UnmodifiableNavigableCollection.EMPTY;
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <K, V> SortedMap<K, V> emptySortedMap() {
        return UnmodifiableNavigableMap.EMPTY;
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Collections"))
    public static <K, V> NavigableMap<K, V> emptyNavigableMap() {
        return UnmodifiableNavigableMap.EMPTY;
    }

}
