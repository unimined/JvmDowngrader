package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.SequencedSet;
import xyz.wagyourtail.jvmdg.j21.impl.UnmodifiableSequencedCollection;
import xyz.wagyourtail.jvmdg.j21.impl.UnmodifiableSequencedMap;
import xyz.wagyourtail.jvmdg.j21.impl.UnmodifiableSequencedSet;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;
import java.util.random.RandomGenerator;

public class J_U_Collections {

    @Stub(ref = @Ref(value = "Ljava/util/Collections;", member = "newSequencedSetFromMap",
        desc = "(Ljava/util/SequencedMap;)Ljava/util/SequencedSet;"))
    public static <E> Set<E> newSequencedSetFromMap(Map<E, Boolean> map) {
        if (!map.isEmpty()) {
            throw new IllegalArgumentException("map must be empty");
        }
        return new SequencedSet<>(map);
    }

    @Stub(ref = @Ref("Ljava/util/Collections;"))
    public static <E> void shuffle(List<E> list, RandomGenerator rng) {
        Collections.shuffle(list, Random.from(rng));
    }

    @Stub(ref = @Ref(value = "Ljava/util/Collections;", member = "unmodifiableSequencedCollection",
        desc = "(Ljava/util/SequencedCollection;)Ljava/util/SequencedCollection;"))
    public static <E> Collection<E> unmodifiableSequencedCollection(Collection<E> c) {
        return new UnmodifiableSequencedCollection<>(c);
    }

    @Stub(ref = @Ref(value = "Ljava/util/Collections;", member = "unmodifiableSequencedMap",
        desc = "(Ljava/util/SequencedMap;)Ljava/util/SequencedMap;"))
    public static <K, V> Map<K, V> unmodifiableSequencedMap(Map<K, V> m) {
        return new UnmodifiableSequencedMap<>(m);
    }

    @Stub(ref = @Ref(value = "Ljava/util/Collections;", member = "unmodifiableSequencedSet",
        desc = "(Ljava/util/SequencedSet;)Ljava/util/SequencedSet;"))
    public static <E> Set<E> unmodifiableSequencedSet(Set<E> s) {
        return new UnmodifiableSequencedSet<>(s);
    }

}

