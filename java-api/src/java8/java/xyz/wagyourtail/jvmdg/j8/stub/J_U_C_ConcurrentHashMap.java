package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiFunction;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class J_U_C_ConcurrentHashMap {

    @Stub
    public static long mappingCount(ConcurrentHashMap<?, ?> map) {
        int size = map.size();
        if (size == Integer.MAX_VALUE) {
            // figure out size, this is slow
            // TODO: speed
            long l = 0;
            Enumeration<?> keys = map.keys();
            while (keys.hasMoreElements()) {
                keys.nextElement();
                l++;
            }
            return l;
        } else {
            return size;
        }
    }

    @Stub(ref = @Ref("java/util/concurrent/ConcurrentHashMap"))
    public static <K> KeySetView<K, Boolean> newKeySet() {
        return new KeySetView<>(new ConcurrentHashMap<K, Boolean>(), Boolean.TRUE);
    }

    @Stub(ref = @Ref("java/util/concurrent/ConcurrentHashMap"))
    public static <K> KeySetView<K, Boolean> newKeySet(int initialCapacity) {
        return new KeySetView<>(new ConcurrentHashMap<K, Boolean>(initialCapacity), Boolean.TRUE);
    }

    @Stub
    public static <K, V> void forEach(ConcurrentHashMap<K, V> map, long parallismThreshold, J_U_F_BiConsumer<? super K, ? super V> action) {
        //TODO
    }

    @Stub
    public static <K, V> void forEach(ConcurrentHashMap<K, V> map, long parallismThreshold, J_U_F_BiFunction<? super K, ? super V, ? extends V> transformer, J_U_F_BiConsumer<? super K, ? super V> action) {
        //TODO
    }

    @Stub
    public static <K, V, U> void search(ConcurrentHashMap<K, V> map, long parallismThreshold, J_U_F_BiFunction<? super K, ? super V, ? extends U> searchFunction) {
        //TODO
    }

    @Stub
    public static <K, V, U> U reduce(ConcurrentHashMap<K, V> map, long parallismThreshold, J_U_F_BiFunction<? super K, ? super V, ? extends U> transformer, J_U_F_BiFunction<? super U, ? super U, ? extends U> reducer) {
        //TODO
    }


    @Stub(ref = @Ref("java/util/concurrent/ConcurrentHashMap$KeySetView"))
    public static class KeySetView<K, V> implements Set<K> {
        private final ConcurrentHashMap<K, V> map;
        private final V value;

        public KeySetView(ConcurrentHashMap<K, V> map, V value) {
            this.map = map;
            this.value = value;
        }


        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return map.containsKey(o);
        }

        @Override
        public Iterator<K> iterator() {
            return map.keySet().iterator();
        }

        @Override
        public Object[] toArray() {
            return map.keySet().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return map.keySet().toArray(a);
        }

        @Override
        public boolean add(K k) {
            return map.put(k, value) == null;
        }

        @Override
        public boolean remove(Object o) {
            return map.remove(o) != null;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return map.keySet().containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends K> c) {
            boolean b = false;
            for (K k : c) {
                b |= add(k);
            }
            return b;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return map.keySet().retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return map.keySet().removeAll(c);
        }

        @Override
        public void clear() {
            map.clear();
        }
    }


}
