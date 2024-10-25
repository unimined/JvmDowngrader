package xyz.wagyourtail.jvmdg.collection;

import xyz.wagyourtail.jvmdg.util.Function;

import java.util.Enumeration;

public class MapEnumeration<T, E> implements Enumeration<E> {
    private final Enumeration<T> enumeration;
    private final Function<T, E> mapper;

    public MapEnumeration(Enumeration<T> enumeration, Function<T, E> mapper) {
        this.enumeration = enumeration;
        this.mapper = mapper;
    }

    @Override
    public boolean hasMoreElements() {
        return enumeration.hasMoreElements();
    }

    @Override
    public E nextElement() {
        if (!hasMoreElements()) {
            return null;
        }
        return mapper.apply(enumeration.nextElement());
    }

}
