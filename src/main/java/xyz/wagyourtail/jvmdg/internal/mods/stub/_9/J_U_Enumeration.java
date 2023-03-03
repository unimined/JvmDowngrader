package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Enumeration;
import java.util.Iterator;

public class J_U_Enumeration {

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true, include = EnumerationIterator.class)
    public static <E> Iterator<E> asIterator(Enumeration<E> self) {
        return new EnumerationIterator<>(self);
    }

    public static class EnumerationIterator<E> implements Iterator<E> {

        private final Enumeration<E> e;

        public EnumerationIterator(Enumeration<E> e) {
            this.e = e;
        }

        @Override
        public boolean hasNext() {
            return e.hasMoreElements();
        }

        @Override
        public E next() {
            return e.nextElement();
        }

    }

}
