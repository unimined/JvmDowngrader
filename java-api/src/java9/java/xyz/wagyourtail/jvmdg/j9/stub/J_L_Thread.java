package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Field;

public class J_L_Thread {

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/lang/Thread;"))
    public static void onSpinWait() {
        Thread.yield();
    }

    @Stub(opcVers = Opcodes.V9, ref = @Ref(value = "Ljava/lang/Thread", member = "<init>"))
    public static Thread init(ThreadGroup group, Runnable target, String name, long stackSize, boolean inheritThreadLocals) throws IllegalAccessException, NoSuchFieldException {
        if (inheritThreadLocals) {
            return new Thread(group, target, name, stackSize);
        } else {
            Thread current = Thread.currentThread();
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
