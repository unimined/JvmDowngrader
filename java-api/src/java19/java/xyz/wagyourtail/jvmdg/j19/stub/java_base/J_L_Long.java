package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Long {

    @Stub(ref = @Ref("Ljava/lang/Long;"))
    public static long compress(long i, long mask) {
        i = i & mask;
        long maskCount = ~mask << 1;

        for (int j = 0; j < 6; j++) {
            long maskPrefix = maskCount ^ (maskCount << 1);
            maskPrefix = maskPrefix ^ (maskPrefix << 2);
            maskPrefix = maskPrefix ^ (maskPrefix << 4);
            maskPrefix = maskPrefix ^ (maskPrefix << 8);
            maskPrefix = maskPrefix ^ (maskPrefix << 16);
            maskPrefix = maskPrefix ^ (maskPrefix << 32);

            long maskMove = maskPrefix & mask;
            mask = (mask ^ maskMove) | (maskMove >>> (1 << j));
            long t = i & maskMove;
            i = (i ^ t) | (t >>> (1 << j));
            maskCount = maskCount & ~maskPrefix;
        }
        return i;
    }

    @Stub(ref = @Ref("Ljava/lang/Long;"))
    public static long expand(long i, long mask) {
        // Save original mask
        long originalMask = mask;
        // Count 0's to right
        long maskCount = ~mask << 1;
        long maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);
        maskPrefix = maskPrefix ^ (maskPrefix << 32);

        // Bits to move
        long maskMove1 = maskPrefix & mask;
        // Compress mask
        mask = (mask ^ maskMove1) | (maskMove1 >>> 1);
        maskCount = maskCount & ~maskPrefix;

        maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);
        maskPrefix = maskPrefix ^ (maskPrefix << 32);
        // Bits to move
        long maskMove2 = maskPrefix & mask;
        // Compress mask
        mask = (mask ^ maskMove2) | (maskMove2 >>> (1 << 1));
        maskCount = maskCount & ~maskPrefix;

        maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);
        maskPrefix = maskPrefix ^ (maskPrefix << 32);
        // Bits to move
        long maskMove3 = maskPrefix & mask;
        // Compress mask
        mask = (mask ^ maskMove3) | (maskMove3 >>> (1 << 2));
        maskCount = maskCount & ~maskPrefix;

        maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);
        maskPrefix = maskPrefix ^ (maskPrefix << 32);
        // Bits to move
        long maskMove4 = maskPrefix & mask;
        // Compress mask
        mask = (mask ^ maskMove4) | (maskMove4 >>> (1 << 3));
        maskCount = maskCount & ~maskPrefix;

        maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);
        maskPrefix = maskPrefix ^ (maskPrefix << 32);
        // Bits to move
        long maskMove5 = maskPrefix & mask;
        // Compress mask
        mask = (mask ^ maskMove5) | (maskMove5 >>> (1 << 4));
        maskCount = maskCount & ~maskPrefix;

        maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);
        maskPrefix = maskPrefix ^ (maskPrefix << 32);
        // Bits to move
        long maskMove6 = maskPrefix & mask;

        long t = i << (1 << 5);
        i = (i & ~maskMove6) | (t & maskMove6);
        t = i << (1 << 4);
        i = (i & ~maskMove5) | (t & maskMove5);
        t = i << (1 << 3);
        i = (i & ~maskMove4) | (t & maskMove4);
        t = i << (1 << 2);
        i = (i & ~maskMove3) | (t & maskMove3);
        t = i << (1 << 1);
        i = (i & ~maskMove2) | (t & maskMove2);
        t = i << 1;
        i = (i & ~maskMove1) | (t & maskMove1);

        // Clear irrelevant bits
        return i & originalMask;
    }

}
