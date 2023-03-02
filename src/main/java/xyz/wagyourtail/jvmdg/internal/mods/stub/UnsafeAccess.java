package xyz.wagyourtail.jvmdg.internal.mods.stub;

import sun.misc.Unsafe;

public class UnsafeAccess {

    private static Unsafe theUnsafe;

    static {
        try {
            var f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            theUnsafe = (Unsafe) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
//        try {
//            var implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
//            privillegedLookup = (MethodHandles.Lookup) theUnsafe.getObject(theUnsafe.staticFieldBase(implLookupField), theUnsafe.staticFieldOffset(implLookupField));
//        } catch (NoSuchFieldException e) {
//            throw new RuntimeException(e);
//        }
    }

    public static <T> T allocateInstance(Class<T> tClass) throws InstantiationException {
        return (T) theUnsafe.allocateInstance(tClass);
    }

    public static void ensureClassInitialized(Class<?> tClass) {
        theUnsafe.ensureClassInitialized(tClass);
    }
}
