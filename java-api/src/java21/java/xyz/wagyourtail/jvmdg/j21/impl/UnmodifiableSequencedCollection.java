package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.jvmdg.j21.stub.java_base.J_U_SequencedCollection;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Unmodifiable sequenced view of a {@link Collection} for
 * {@code Collections.unmodifiableSequencedCollection}. Recognized as sequenced
 * via {@link J_U_SequencedCollection.Marker} and throws
 * {@link UnsupportedOperationException} from all mutating operations.
 */
public class UnmodifiableSequencedCollection<E, T extends Collection<E>> extends AbstractCollection<E> implements J_U_SequencedCollection.Marker {
    public final T original;

    public UnmodifiableSequencedCollection(T original) {
        this.original = original;
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        Iterator<E> it = original.iterator();
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                return it.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int size() {
        return original.size();
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

}
