package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Integer {

    @Stub(ref = @Ref("Ljava/lang/Integer;"))
    public static int compress(int i, int mask) {
        i = i & mask;
        int maskCount = ~mask << 1;

        for (int j = 0; j < 5; j++) {
            int maskPrefix = maskCount ^ (maskCount << 1);
            maskPrefix = maskPrefix ^ (maskPrefix << 2);
            maskPrefix = maskPrefix ^ (maskPrefix << 4);
            maskPrefix = maskPrefix ^ (maskPrefix << 8);
            maskPrefix = maskPrefix ^ (maskPrefix << 16);

            int maskMove = maskPrefix & mask;
            mask = (mask ^ maskMove) | (maskMove >>> (1 << j));
            int t = i & maskMove;
            i = (i ^ t) | (t >>> (1 << j));
            maskCount = maskCount & ~maskPrefix;
        }
        return i;
    }

    @Stub(ref = @Ref("Ljava/lang/Integer;"))
    public static int expand(int i, int mask) {
        // Save original mask
        int originalMask = mask;
        // Count 0's to right
        int maskCount = ~mask << 1;

        int maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);

        // Bits to move
        int maskMove1 = maskPrefix & mask;
        // Compress mask
        mask = (mask ^ maskMove1) | (maskMove1 >>> 1);
        maskCount = maskCount & ~maskPrefix;

        maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);
        // Bits to move
        int maskMove2 = maskPrefix & mask;
        // Compress mask
        mask = (mask ^ maskMove2) | (maskMove2 >>> (1 << 1));
        maskCount = maskCount & ~maskPrefix;

        maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);
        // Bits to move
        int maskMove3 = maskPrefix & mask;
        // Compress mask
        mask = (mask ^ maskMove3) | (maskMove3 >>> (1 << 2));
        maskCount = maskCount & ~maskPrefix;

        maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);

        // Bits to move
        int maskMove4 = maskPrefix & mask;
        // Compress mask
        mask = (mask ^ maskMove4) | (maskMove4 >>> (1 << 3));
        maskCount = maskCount & ~maskPrefix;

        maskPrefix = maskCount ^ (maskCount << 1);
        maskPrefix = maskPrefix ^ (maskPrefix << 2);
        maskPrefix = maskPrefix ^ (maskPrefix << 4);
        maskPrefix = maskPrefix ^ (maskPrefix << 8);
        maskPrefix = maskPrefix ^ (maskPrefix << 16);

        // Bits to move
        int maskMove5 = maskPrefix & mask;

        int t = i << (1 << 4);
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
