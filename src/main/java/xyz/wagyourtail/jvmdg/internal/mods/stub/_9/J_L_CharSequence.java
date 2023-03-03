package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Iterator;
import java.util.stream.IntStream;

public class J_L_CharSequence {

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static IntStream chars(CharSequence cs) {
        return IntStream.range(0, cs.length()).map(cs::charAt);
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true, include = CodePointIterator.class)
    public static IntStream codePoints(CharSequence cs) {
        return new CodePointIterator(cs).stream();
    }

    public static class CodePointIterator {

        private final CharSequence cs;
        private int index = 0;

        public CodePointIterator(CharSequence cs) {
            this.cs = cs;
        }

        public boolean hasNext() {
            return cs.length() > 0;
        }


        public Integer next() {
            int cp = cs.charAt(index++);
            if (Character.isHighSurrogate((char) cp) && cs.length() > index) {
                int cp2 = cs.charAt(index++);
                if (Character.isLowSurrogate((char) cp2)) {
                    cp = Character.toCodePoint((char) cp, (char) cp2);
                } else {
                    index--;
                }
            }
            return cp;
        }

        public boolean takeWhile(int inp) {
            return this.hasNext();
        }

        public IntStream stream() {
            return IntStream.generate(this::next).takeWhile(this::takeWhile);
        }
    }
}
