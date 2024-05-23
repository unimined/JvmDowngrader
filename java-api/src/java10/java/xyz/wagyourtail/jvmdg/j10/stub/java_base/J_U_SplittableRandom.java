package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.SplittableRandom;

public class J_U_SplittableRandom {

    @Stub
    public static void nextBytes(SplittableRandom random, byte[] bytes) {
        int i = 0;
        int len = bytes.length;
        int words = len >> 3;
        while (words-- > 0) {
            long rnd = random.nextLong();
            for (int n = 0; n < 8; n++) {
                bytes[i++] = (byte) rnd;
                rnd >>>= Byte.SIZE;
            }
        }
        if (i < len) {
            long rnd = random.nextLong();
            for (int n = 0; n < len - i; n++) {
                bytes[i++] = (byte) rnd;
                rnd >>>= Byte.SIZE;
            }
        }
    }

}
