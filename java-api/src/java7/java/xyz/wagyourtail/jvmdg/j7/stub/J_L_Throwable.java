package xyz.wagyourtail.jvmdg.j7.stub;

import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;

public class J_L_Throwable {
    private static final Map<Throwable, List<Throwable>> suppressed = Collections.synchronizedMap(new WeakHashMap<Throwable, List<Throwable>>());
    private static final Throwable[] empty = new Throwable[0];

    @Stub
    public static void addSuppressed(Throwable self, Throwable other) {
        if (!suppressed.containsKey(self)) {
            synchronized (suppressed) {
                if (!suppressed.containsKey(self)) {
                    suppressed.put(self, new ArrayList<Throwable>());
                }
            }
        }
        suppressed.get(self).add(other);
    }

    @Stub
    public static Throwable[] getSuppressed(Throwable self) {
        if (!suppressed.containsKey(self)) {
            synchronized (suppressed) {
                if (!suppressed.containsKey(self)) {
                    return empty;
                }
            }
        }
        return suppressed.get(self).toArray(empty);
    }

}
