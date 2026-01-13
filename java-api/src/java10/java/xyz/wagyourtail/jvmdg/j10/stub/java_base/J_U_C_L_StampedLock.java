package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_U_C_L_StampedLock {
    private static final int LG_READERS = 7;

    private static final long WBIT  = 1L << LG_READERS;
    private static final long RBITS = WBIT - 1L;
    private static final long ABITS = RBITS | WBIT;

    @Stub(ref = @Ref("java/util/concurrent/locks/StampedLock"))
    public static boolean isWriteLockStamp(long stamp) {
        return (stamp & ABITS) == WBIT;
    }

    @Stub(ref = @Ref("java/util/concurrent/locks/StampedLock"))
    public static boolean isReadLockStamp(long stamp) {
        return (stamp & RBITS) != 0L;
    }

    @Stub(ref = @Ref("java/util/concurrent/locks/StampedLock"))
    public static boolean isLockStamp(long stamp) {
        return (stamp & ABITS) != 0L;
    }

    @Stub(ref = @Ref("java/util/concurrent/locks/StampedLock"))
    public static boolean isOptimisticReadStamp(long stamp) {
        return (stamp & ABITS) == 0L && stamp != 0L;
    }

}
