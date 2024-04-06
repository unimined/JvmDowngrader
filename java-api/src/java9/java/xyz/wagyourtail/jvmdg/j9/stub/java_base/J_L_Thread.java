package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class J_L_Thread {

    @Stub(ref = @Ref("Ljava/lang/Thread;"))
    public static void onSpinWait() {
        Thread.yield();
    }

    @Modify(javaVersion = Opcodes.V9, ref = @Ref(value = "java/lang/Thread", member = "<init>", desc = "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;JZ)V"))
    public static void init(MethodNode mnode, int i) {
        AbstractInsnNode node = mnode.instructions.get(i);
        InsnList list = new InsnList();

        // stack: Thread, ThreadGroup, Runnable, String, long, boolean
        // call preInit
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(J_L_Thread.class).getInternalName(), "preInit", "(Z)V", false));
        // stack: Thread, ThreadGroup, Runnable, String, long
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Thread", "<init>", "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;J)V", false));
        // stack: Thread
        // call postInit
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(J_L_Thread.class).getInternalName(), "postInit", "()V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    private static final ThreadLocal<Object> old = new ThreadLocal<>();

    public static void preInit(boolean inheritThreadLocals) {
        if (!inheritThreadLocals) {
            Thread current = Thread.currentThread();
            try {
                MethodHandles.Lookup impl_lookup = Utils.getImplLookup();
                Class<?> map = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
                MethodHandle setter = impl_lookup.findSetter(Thread.class, "inheritableThreadLocals", map);
                MethodHandle getter = impl_lookup.findGetter(Thread.class, "inheritableThreadLocals", map);
                old.set(getter.invoke(current));
                setter.invoke(current, null);
            } catch (Throwable e) {
                throw new Error(e);
            }
        } else {
            old.set(null);
        }
    }

    public static void postInit() {
        Object o = old.get();
        if (o != null) {
            Thread current = Thread.currentThread();
            try {
                MethodHandles.Lookup impl_lookup = Utils.getImplLookup();
                Class<?> map = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
                MethodHandle setter = impl_lookup.findSetter(Thread.class, "inheritableThreadLocals", map);
                setter.invoke(current, o);
            } catch (Throwable e) {
                throw new Error(e);
            }
        }
    }

}
