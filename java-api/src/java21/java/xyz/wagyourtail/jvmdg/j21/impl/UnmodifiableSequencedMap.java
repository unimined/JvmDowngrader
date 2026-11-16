package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Unmodifiable sequenced view of a {@link Map} for
 * {@code Collections.unmodifiableSequencedMap}. Recognized as sequenced by
 * {@code J_U_SequencedMap.jvmdg$instanceof} and throws
 * {@link UnsupportedOperationException} from all mutating operations,
 * matching the JDK's unmodifiable wrappers.
 */
public class UnmodifiableSequencedMap<K, V, T extends Map<K, V>> implements Map<K, V> {
    public final T original;

    public UnmodifiableSequencedMap(T original) {
        this.original = original;
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(original.entrySet());
    }

    @Override
    public int size() {
        return original.size();
    }

    @Override
    public boolean isEmpty() {
        return original.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return original.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return original.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return original.get(key);
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(original.keySet());
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(original.values());
    }

    @Override
    public String toString() {
        return original.toString();
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
