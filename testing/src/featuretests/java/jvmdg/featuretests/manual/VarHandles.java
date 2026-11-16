package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * VarHandle access modes: plain/volatile/acquire-release/opaque, CAS,
 * compareAndExchange, getAndSet, bitwise ops on field VarHandles for
 * boolean/char/short — the j9 stub family implements per-kind impl classes.
 */
public class VarHandles {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    VarHandles[] tvh;
    boolean b;
    char c;
    short s;
    int i;
    volatile int vi;
    long l;
    float f;
    double d;

    boolean[] ba;
    char[] ca;
    short[] sa;
    int[] ia;
    long[] la;
    float[] fa;
    double[] da;

    public static void main(String[] args) {
        Support.run(VarHandles.class);
    }

    @FeatureTest
    static void all() throws Throwable {
        // field VarHandles per primitive type through every access mode —
        // the j9 stub family picks per-kind impl classes at lookup time
        VarHandles tvh = new VarHandles();
        testBoolean(tvh);
        testChar(tvh);
        testShort(tvh);
        VarHandle ih = lookup.findVarHandle(VarHandles.class, "i", int.class);
        VarHandle lh = lookup.findVarHandle(VarHandles.class, "l", long.class);
        VarHandle fh = lookup.findVarHandle(VarHandles.class, "f", float.class);
        VarHandle dh = lookup.findVarHandle(VarHandles.class, "d", double.class);
        ih.set(tvh, 42);
        System.out.println(ih.get(tvh));
        lh.set(tvh, 43L);
        System.out.println(lh.get(tvh));
        fh.set(tvh, 1.5f);
        System.out.println(fh.get(tvh));
        dh.set(tvh, 2.5d);
        System.out.println(dh.get(tvh));
        // ACC_VOLATILE field through the volatile access modes
        VarHandle vh = lookup.findVarHandle(VarHandles.class, "vi", int.class);
        vh.setVolatile(tvh, 42);
        System.out.println(vh.getVolatile(tvh));
        System.out.println(vh.compareAndSet(tvh, 42, 43));
        System.out.println(vh.getAcquire(tvh));
    }

    public static void testBoolean(VarHandles tvh) throws NoSuchFieldException, IllegalAccessException {
        VarHandle bh = lookup.findVarHandle(VarHandles.class, "b", boolean.class);
        System.out.println((boolean) bh.get(tvh));
        bh.set(tvh, true);
        System.out.println(tvh.b);
        System.out.println((boolean) bh.getVolatile(tvh));
        bh.setVolatile(tvh, false);
        System.out.println(tvh.b);
        System.out.println((boolean) bh.getAcquire(tvh));
        bh.setRelease(tvh, true);
        System.out.println(tvh.b);
        System.out.println((boolean) bh.getOpaque(tvh));
        bh.setOpaque(tvh, false);
        System.out.println(tvh.b);
        System.out.println(bh.compareAndSet(tvh, true, false));
        System.out.println(tvh.b);
        System.out.println(bh.compareAndSet(tvh, false, true));
        System.out.println(tvh.b);
        System.out.println(bh.compareAndExchange(tvh, true, false));
        System.out.println(tvh.b);
        System.out.println(bh.compareAndExchange(tvh, false, true));
        System.out.println(tvh.b);
        System.out.println((boolean) bh.getAndSet(tvh, false));
        System.out.println(tvh.b);
        System.out.println((boolean) bh.getAndSet(tvh, true));
        System.out.println(tvh.b);
        System.out.println((boolean) bh.getAndBitwiseOr(tvh, true));
        System.out.println(tvh.b);
        System.out.println((boolean) bh.getAndBitwiseAnd(tvh, false));
        System.out.println(tvh.b);
        System.out.println((boolean) bh.getAndBitwiseXor(tvh, true));
        System.out.println(tvh.b);
        System.out.println((boolean) bh.getAndBitwiseXor(tvh, true));
        System.out.println(tvh.b);
    }

    public static void testChar(VarHandles tvh) throws NoSuchFieldException, IllegalAccessException {
        VarHandle ch = lookup.findVarHandle(VarHandles.class, "c", char.class);
        System.out.println(ch.get(tvh));
        ch.set(tvh, 'c');
        System.out.println(tvh.c);
        System.out.println((char) ch.getVolatile(tvh));
        ch.setVolatile(tvh, 'e');
        System.out.println(tvh.c);
        System.out.println((char) ch.getAcquire(tvh));
        ch.setRelease(tvh, 'a');
        System.out.println(tvh.c);
        System.out.println((char) ch.getOpaque(tvh));
        ch.setOpaque(tvh, 'z');
        System.out.println(ch.compareAndSet(tvh, 'a', 'z'));
        System.out.println(tvh.c);
        System.out.println((char) ch.compareAndExchange(tvh, 'z', 'w'));
        System.out.println(tvh.c);
        System.out.println((char) ch.getAndSet(tvh, 'z'));
        System.out.println(tvh.c);
        System.out.println((char) ch.getAndSet(tvh, 'z'));
        System.out.println(tvh.c);
        System.out.println((char) ch.getAndBitwiseOr(tvh, 'a'));
        System.out.println(tvh.b);
        System.out.println((char) ch.getAndBitwiseOr(tvh, 'A'));
        System.out.println(tvh.b);
        System.out.println((char) ch.getAndBitwiseXor(tvh, 'Z'));
        System.out.println(tvh.b);
        System.out.println(ch.getAndBitwiseXor(tvh, 'W'));
        System.out.println(tvh.b);
    }

    public static void testShort(VarHandles tvh) throws NoSuchFieldException, IllegalAccessException {
        VarHandle sh = lookup.findVarHandle(VarHandles.class, "s", short.class);
        System.out.println((short) sh.get(tvh));
        sh.set(tvh, (short) 13891);
        System.out.println(tvh.s);
        System.out.println(sh.getVolatile(tvh));
        sh.setVolatile(tvh, (short) 573);
        System.out.println(tvh.s);
        System.out.println((short) sh.getAcquire(tvh));
        sh.setRelease(tvh, (short) 109);
        System.out.println(sh.compareAndSet(tvh, (short) 13891, (short) 573));
        System.out.println(tvh.s);
        System.out.println(sh.compareAndSet(tvh, (short) 109, (short) 567));
        System.out.println(tvh.s);
        System.out.println((short) sh.compareAndExchange(tvh, (short) 567, (short) 571));
        System.out.println(tvh.s);
        System.out.println((short) sh.getAndBitwiseOr(tvh, (short) 2356));
        System.out.println(tvh.s);
        System.out.println(sh.getAndBitwiseAnd(tvh, (short) 547));
        System.out.println(tvh.s);
        System.out.println((short) sh.getAndBitwiseXor(tvh, (short) 2356));
        System.out.println(tvh.s);
        System.out.println((short) sh.getAndBitwiseXor(tvh, (short) 547));
        System.out.println(tvh.s);
    }

}
