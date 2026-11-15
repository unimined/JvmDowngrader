package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.exc.PartialStubError;
import xyz.wagyourtail.jvmdg.j21.impl.ReverseCollection;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;

@JEP(431)
@Adapter(value = "java/util/SequencedCollection", target = "java/util/Collection")
public class J_U_SequencedCollection {

    /**
     * Opt-in {@code java/util/SequencedCollection} membership for types that
     * implement neither the real interface nor this stub — jvmdg's sequenced
     * impls ({@code ReverseCollection}, {@code UnmodifiableSequencedCollection})
     * declare it here; the {@code RetroTypes} table lists it for every adapter.
     */
    public interface Marker {
    }

    private J_U_SequencedCollection() {
    }

    @Stub
    public static <E> Collection<E> reversed(Collection<E> self) {
        if (self instanceof List<E> list) {
            return J_U_List.reversed(list);
        }
        if (self instanceof Deque<E> deque) {
            return J_U_Deque.reversed(deque);
        }
        if (self instanceof Set<E> set) {
            return J_U_SequencedSet.reversed(set);
        }
        if (self != null) {
            return new ReverseCollection<>(self);
        }
        throw PartialStubError.create();
    }

    @Stub
    public static <E> void addFirst(Collection<E> self, E e) {
        if (self instanceof ReverseCollection<E, ?> rev) {
            addLast(rev.original, e);
            return;
        }
        if (self instanceof List<E> list) {
            list.add(0, e);
            return;
        } else if (self instanceof Deque<E> deque) {
            deque.addFirst(e);
            return;
        }
        throw PartialStubError.create();
    }

    @Stub
    public static <E> void addLast(Collection<E> self, E e) {
        if (self instanceof ReverseCollection<E, ?> rev) {
            addFirst(rev.original, e);
            return;
        }
        if (self instanceof List<E> list) {
            list.add(e);
            return;
        } else if (self instanceof Deque<E> deque) {
            deque.addLast(e);
            return;
        } else if (self instanceof LinkedHashSet<E>) {
            self.add(e);
            return;
        }
        throw PartialStubError.create();
    }

    @Stub
    public static <E> E getFirst(Collection<E> self) {
        if (self.isEmpty()) {
            throw new NoSuchElementException();
        }
        if (self instanceof List<E> list) {
            return list.get(0);
        } else if (self instanceof Deque<E> deque) {
            return deque.getFirst();
        }
        return self.iterator().next();
    }

    @Stub
    public static <E> E getLast(Collection<E> self) {
        if (self.isEmpty()) {
            throw new NoSuchElementException();
        }
        if (self instanceof List<E> list) {
            return list.get(list.size() - 1);
        } else if (self instanceof Deque<E> deque) {
            return deque.getLast();
        }
        return reversed(self).iterator().next();
    }

    /**
     * immutable collections ({@code List.of} family —
     * {@code java.util.ImmutableCollections} on newer runtimes, our
     * {@code ImmutableList} stand-in elsewhere) override the mutating
     * sequenced operations to throw unconditionally; everything else throws
     * {@link NoSuchElementException} when empty.
     */
    public static void checkEmptyRemovable(Collection<?> self) {
        if (self.isEmpty()) {
            if (self instanceof xyz.wagyourtail.jvmdg.j9.intl.ImmutableList
                || self.getClass().getName().startsWith("java.util.ImmutableCollections")) {
                throw new UnsupportedOperationException();
            }
            throw new NoSuchElementException();
        }
    }

    @Stub
    public static <E> E removeFirst(Collection<E> self) {
        checkEmptyRemovable(self);
        if (self instanceof List<E> list) {
            return list.remove(0);
        } else if (self instanceof Deque<E> deque) {
            return deque.removeFirst();
        }
        Iterator<E> it = self.iterator();
        E e = it.next();
        it.remove();
        return e;
    }

    @Stub
    public static <E> E removeLast(Collection<E> self) {
        checkEmptyRemovable(self);
        if (self instanceof List<E> list) {
            return list.remove(list.size() - 1);
        } else if (self instanceof Deque<E> deque) {
            return deque.removeLast();
        }
        Iterator<E> it = reversed(self).iterator();
        E e = it.next();
        it.remove();
        return e;
    }

}
