package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitPre;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

@InitRewrite(ref = @Ref(value = "Ljava/lang/Thread;", member = "<init>", desc = "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;JZ)V"), to = "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;J)V", keep = {0, 1, 2, 3})
public class J_L_Thread {

    private static final ThreadLocal<Object> old = new ThreadLocal<>();
    private static final MethodHandles.Lookup impl_lookup = Utils.getImplLookup();

    @Stub(ref = @Ref("Ljava/lang/Thread;"))
    public static void onSpinWait() {
        Thread.yield();
    }

    // runs before the retargeted <init> with the dropped inheritThreadLocals arg
    @InitPre(target = "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;JZ)V", from = {4})
    public static void preInit(boolean inheritThreadLocals) {
        if (!inheritThreadLocals) {
            Thread current = Thread.currentThread();
            try {
                Class<?> map = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
                MethodHandle setter = impl_lookup.findSetter(Thread.class, "inheritableThreadLocals", map);
                MethodHandle getter = impl_lookup.findGetter(Thread.class, "inheritableThreadLocals", map);
                old.set(getter.invoke(current));
                setter.invoke(current, null);
            } catch (Throwable e) {
                throw new Error(e);
            }
        } else {
            old.remove();
        }
    }

    // postInit restores state on the *current* thread — the Self param is unused
    @InitPost(target = "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;JZ)V")
    public static void post(Thread self) {
        postInit();
    }

    public static void postInit() {
        Object o = old.get();
        if (o != null) {
            Thread current = Thread.currentThread();
            try {
                Class<?> map = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
                MethodHandle setter = impl_lookup.findSetter(Thread.class, "inheritableThreadLocals", map);
                setter.invoke(current, o);
            } catch (Throwable e) {
                throw new Error(e);
            }
        }
    }

}
