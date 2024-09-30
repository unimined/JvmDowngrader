package xyz.wagyourtail.downgradetest;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class TestVarHandle {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    TestVarHandle[] tvh;
    boolean b;
    char c;
    short s;
    int i;
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

    public static void main(String[] args) throws Throwable {
        TestVarHandle tvh = new TestVarHandle();
        testBoolean(tvh);
        testChar(tvh);
        testShort(tvh);
        VarHandle ih = lookup.findVarHandle(TestVarHandle.class, "i", int.class);
        VarHandle lh = lookup.findVarHandle(TestVarHandle.class, "l", long.class);
        VarHandle fh = lookup.findVarHandle(TestVarHandle.class, "f", float.class);
        VarHandle dh = lookup.findVarHandle(TestVarHandle.class, "d", double.class);


    }

    public static void testBoolean(TestVarHandle tvh) throws NoSuchFieldException, IllegalAccessException {
        VarHandle bh = lookup.findVarHandle(TestVarHandle.class, "b", boolean.class);
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

    public static void testChar(TestVarHandle tvh) throws NoSuchFieldException, IllegalAccessException {
        VarHandle ch = lookup.findVarHandle(TestVarHandle.class, "c", char.class);
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

    public static void testShort(TestVarHandle tvh) throws NoSuchFieldException, IllegalAccessException {
        VarHandle sh = lookup.findVarHandle(TestVarHandle.class, "s", short.class);
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
