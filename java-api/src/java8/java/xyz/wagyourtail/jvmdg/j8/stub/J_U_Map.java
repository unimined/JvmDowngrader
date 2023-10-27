package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiFunction;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Function;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

public class J_U_Map {

    @Stub(ref = @Ref("java/util/Map"))
    public static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K, V>> comparingByKey() {
        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        };
    }

    @Stub(ref = @Ref("java/util/Map"))
    public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> comparingByValue() {
        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        };
    }

    @Stub(ref = @Ref("java/util/Map"))
    public static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(final Comparator<? super K> cmp) {
        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return cmp.compare(o1.getKey(), o2.getKey());
            }
        };
    }

    @Stub(ref = @Ref("java/util/Map"))
    public static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(final Comparator<? super V> cmp) {
        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return cmp.compare(o1.getValue(), o2.getValue());
            }
        };
    }

    @Stub
    public <K, V> V getOrDefault(Map<K, V> self, Object key, V defaultValue) {
        V v;
        return (((v = self.get(key)) != null) || self.containsKey(key))
                ? v
                : defaultValue;
    }

    @Stub
    public <K, V> void forEach(Map<K, V> self, J_U_F_BiConsumer<? super K, ? super V> action) {
        for (Map.Entry<K, V> entry : self.entrySet()) {
            K k;
            V v;
            action.accept(((k = entry.getKey()) != null) ? k : null, ((v = entry.getValue()) != null) ? v : null);
        }
    }

    @Stub
    public <T, U> void replaceAll(Map<T, U> self, J_U_F_BiFunction<T, U, U> function) {
        for (Map.Entry<T, U> entry : self.entrySet()) {
            T k;
            U v;
            entry.setValue(function.apply(((k = entry.getKey()) != null) ? k : null, ((v = entry.getValue()) != null) ? v : null));
        }
    }

    @Stub
    public <K, V> V putIfAbsent(Map<K, V> self, K key, V value) {
        V v = self.get(key);
        if (v == null) {
            v = self.put(key, value);
        }
        return v;
    }

    @Stub
    public <K, V> boolean remove(Map<K, V> self, Object key, Object value) {
        Object curValue = self.get(key);
        if (!Objects.equals(curValue, value) ||
                (curValue == null && !self.containsKey(key))) {
            return false;
        }
        self.remove(key);
        return true;
    }

    @Stub
    public <K, V> boolean replace(Map<K, V> self, K key, V oldValue, V newValue) {
        Object curValue = self.get(key);
        if (!Objects.equals(curValue, oldValue) ||
                (curValue == null && !self.containsKey(key))) {
            return false;
        }
        self.put(key, newValue);
        return true;
    }

    @Stub
    public <K, V> V replace(Map<K, V> self, K key, V value) {
        V curValue;
        if (((curValue = self.get(key)) != null) || self.containsKey(key)) {
            curValue = self.put(key, value);
        }
        return curValue;
    }

    @Stub
    public <K, V> V computeIfAbsent(Map<K, V> self, K key, J_U_F_Function<? super K, ? extends V> mappingFunction) {
        V v;
        if ((v = self.get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                self.put(key, newValue);
                return newValue;
            }
        }
        return v;
    }

    @Stub
    public <K, V> V computeIfPresent(Map<K, V> self, K key, J_U_F_BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V oldValue;
        if ((oldValue = self.get(key)) != null) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                self.put(key, newValue);
                return newValue;
            } else {
                self.remove(key);
                return null;
            }
        } else {
            return null;
        }
    }

    @Stub
    public <K, V> V compute(Map<K, V> self, K key, J_U_F_BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V oldValue = self.get(key);
        V newValue = remappingFunction.apply(key, oldValue);
        if (newValue == null) {
            if (oldValue != null || self.containsKey(key)) {
                self.remove(key);
                return null;
            } else {
                return null;
            }
        } else {
            self.put(key, newValue);
            return newValue;
        }
    }

    @Stub
    public <K, V> V merge(Map<K, V> self, K key, V value, J_U_F_BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        V oldValue = self.get(key);
        V newValue = (oldValue == null) ? value :
                remappingFunction.apply(oldValue, value);
        if (newValue == null) {
            self.remove(key);
        } else {
            self.put(key, newValue);
        }
        return newValue;
    }

}
