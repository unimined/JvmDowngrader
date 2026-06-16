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

    private J_U_SequencedCollection() {
    }

    public static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof List<?> ||
            obj instanceof LinkedHashSet<?> ||
            obj instanceof Deque<?> ||
            obj instanceof SortedSet<?> ||
            obj instanceof ReverseCollection<?, ?> ||
            obj.getClass().getDeclaringClass().isAssignableFrom(LinkedHashMap.class);
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

    @Stub(excludeChild = "java/util/Deque")
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

    @Stub(excludeChild = "java/util/Deque")
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

    @Stub(excludeChild = "java/util/Deque")
    public static <E> E getFirst(Collection<E> self) {
        if (self instanceof List<E> list) {
            return list.get(0);
        } else if (self instanceof Deque<E> deque) {
            return deque.getFirst();
        }
        return self.iterator().next();
    }

    @Stub(excludeChild = "java/util/Deque")
    public static <E> E getLast(Collection<E> self) {
        if (self instanceof List<E> list) {
            return list.get(list.size() - 1);
        } else if (self instanceof Deque<E> deque) {
            return deque.getLast();
        }
        return reversed(self).iterator().next();
    }

    @Stub(excludeChild = "java/util/Deque")
    public static <E> E removeFirst(Collection<E> self) {
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

    @Stub(excludeChild = "java/util/Deque")
    public static <E> E removeLast(Collection<E> self) {
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
