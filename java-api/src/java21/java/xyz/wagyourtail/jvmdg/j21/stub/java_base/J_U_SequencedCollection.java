package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j21.impl.ReverseDeque;
import xyz.wagyourtail.jvmdg.j21.impl.ReverseList;
import xyz.wagyourtail.jvmdg.j21.impl.ReverseSet;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;

@Adapter(value = "java/util/SequencedCollection", target = "java/util/Collection")
public class J_U_SequencedCollection {

    public static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof List<?> ||
            obj instanceof LinkedHashSet<?> ||
            obj instanceof Deque<?> ||
            obj instanceof SortedSet<?>;
    }

    public static <E> Collection<E> jvmdg$checkcast(Object obj) {
        if (!(obj instanceof Collection<?>)) {
            throw new ClassCastException();
        }
        return (Collection<E>) obj;
    }

    @Stub
    public static <E> Collection<E> reversed(Collection<E> self) {
        if (self instanceof List<E> list) {
            if (list instanceof ReverseList<E> rl) {
                return rl.original;
            }
            return new ReverseList<>(list);
        }
        if (self instanceof Deque<E> deque) {
            if (deque instanceof ReverseDeque<E> rd) {
                return rd.original;
            }
            return new ReverseDeque<>(deque);
        }
        if (self instanceof Set<E> set) {
            if (set instanceof ReverseSet<E> rs) {
                return rs.original;
            }
            return new ReverseSet<>(set);
        }
        throw new MissingStubError("java.util.SequencedCollection.reversed not implemented for " + self.getClass().getName());
    }

    @Stub
    public static <E> void addFirst(Collection<E> self, E e) {
        throw new UnsupportedOperationException();
    }

    public static <E> void addLast(Collection<E> self, E e) {
        throw new UnsupportedOperationException();
    }

    public static <E> E getFirst(Collection<E> self) {
        return self.iterator().next();
    }

    public static <E> E getLast(Collection<E> self) {
        return reversed(self).iterator().next();
    }

    public static <E> E removeFirst(Collection<E> self) {
        Iterator<E> it = self.iterator();
        E e = it.next();
        it.remove();
        return e;
    }

    public static <E> E removeLast(Collection<E> self) {
        Iterator<E> it = reversed(self).iterator();
        E e = it.next();
        it.remove();
        return e;
    }


}
