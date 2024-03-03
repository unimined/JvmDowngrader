package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Enumeration;
import java.util.Iterator;

public class J_U_Enumeration {

    @Stub
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
