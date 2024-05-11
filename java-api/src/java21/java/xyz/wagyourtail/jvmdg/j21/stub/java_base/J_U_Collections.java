package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.SequencedSet;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;
import java.util.random.RandomGenerator;

public class J_U_Collections {

    @Stub(ref = @Ref("Ljava/util/Collections;"))
    public static <E> Set<E> newSequencedSetFromMap(Map<E, Boolean> map) {
        return new SequencedSet<>(map);
    }

    @Stub(ref = @Ref("Ljava/util/Collections;"))
    public static <E> void shuffle(List<E> list, RandomGenerator rng) {
        Collections.shuffle(list, Random.from(rng));
    }

    @Stub(ref = @Ref("Ljava/util/Collections;"))
    public static <E> Collection<E> unmodifiableSequencedCollection(Collection<E> c) {
        return Collections.unmodifiableCollection(c);
    }

    @Stub(ref = @Ref("Ljava/util/Collections;"))
    public static <K, V> Map<K, V> unmodifiableSequencedMap(Map<K, V> m) {
        return Collections.unmodifiableMap(m);
    }

    @Stub(ref = @Ref("Ljava/util/Collections;"))
    public static <E> Set<E> unmodifiableSequencedSet(Set<E> s) {
        return Collections.unmodifiableSet(s);
    }

}

