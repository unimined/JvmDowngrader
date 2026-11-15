package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.ReverseSet;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;

@JEP(431)
@Adapter(value = "java/util/SequencedSet", target = "java/util/Set")
public class J_U_SequencedSet {

    /**
     * Opt-in {@code java/util/SequencedSet} membership for types that
     * implement neither the real interface nor this stub — see
     * {@link J_U_SequencedCollection.Marker}.
     */
    public interface Marker {
    }

    private J_U_SequencedSet() {
    }

    @Stub
    public static <E> Set<E> reversed(Set<E> self) {
        if (self instanceof NavigableSet<E>) {
            return ((NavigableSet<E>) self).descendingSet();
        }
        if (self instanceof ReverseSet<E, ?> rs) {
            return rs.original;
        }
        return ReverseSet.create(self);
    }

}
