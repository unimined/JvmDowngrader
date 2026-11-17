package xyz.wagyourtail.jvmdg.j21.impl;

import xyz.wagyourtail.jvmdg.j21.stub.java_base.J_U_SequencedSet;

import java.util.Set;

/**
 * Unmodifiable sequenced view of a {@link Set} for
 * {@code Collections.unmodifiableSequencedSet}. Recognized as sequenced
 * via {@link J_U_SequencedSet.Marker} and throws
 * {@link UnsupportedOperationException} from all mutating operations.
 */
public class UnmodifiableSequencedSet<E, T extends Set<E>> extends UnmodifiableSequencedCollection<E, T> implements Set<E>, J_U_SequencedSet.Marker {

    public UnmodifiableSequencedSet(T original) {
        super(original);
    }

    @Override
    public boolean equals(Object o) {
        return original.equals(o);
    }

    @Override
    public int hashCode() {
        return original.hashCode();
    }

}
