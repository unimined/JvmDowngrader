package xyz.wagyourtail.jvmdg.j21.impl;

import java.util.Set;

/**
 * Unmodifiable sequenced view of a {@link Set} for
 * {@code Collections.unmodifiableSequencedSet}. Recognized as sequenced by
 * {@code J_U_SequencedSet.jvmdg$instanceof} and throws
 * {@link UnsupportedOperationException} from all mutating operations.
 */
public class UnmodifiableSequencedSet<E, T extends Set<E>> extends UnmodifiableSequencedCollection<E, T> implements Set<E> {

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
