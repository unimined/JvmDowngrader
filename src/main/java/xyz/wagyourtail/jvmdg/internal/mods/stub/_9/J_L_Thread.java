package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.lang.reflect.Field;
import java.util.Objects;

public class J_L_Thread {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/Thread;")
    public static void onSpinWait() {
        Thread.yield();
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/Thread;<>")
    public static Thread init(ThreadGroup group, Runnable target, String name, long stackSize, boolean inheritThreadLocals) throws IllegalAccessException, NoSuchFieldException {
        if (inheritThreadLocals) {
            return new Thread(group, target, name, stackSize);
        } else {
            var current = Thread.currentThread();
            Field f = Thread.class.getDeclaredField("inheritableThreadLocals");
            f.setAccessible(true);
            Object old = f.get(current);
            f.set(current, null);
            try {
                return new Thread(group, target, name, stackSize);
            } finally {
                f.set(current, old);
            }
        }
    }
}
