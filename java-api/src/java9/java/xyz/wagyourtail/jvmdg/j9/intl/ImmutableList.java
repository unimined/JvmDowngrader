package xyz.wagyourtail.jvmdg.j9.intl;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

/**
 * Minimal stand-in for {@code java.util.ImmutableCollections} lists
 * (returned by {@code List.of}/{@code List.copyOf} on newer JDKs):
 * fully unmodifiable — every mutating op throws
 * {@link UnsupportedOperationException} via {@link AbstractList}'s defaults.
 */
public class ImmutableList<E> extends AbstractList<E> implements RandomAccess {

    private final List<E> backing;

    public ImmutableList(List<E> backing) {
        this.backing = backing;
    }

    @Override
    public E get(int index) {
        return backing.get(index);
    }

    @Override
    public int size() {
        return backing.size();
    }

}
