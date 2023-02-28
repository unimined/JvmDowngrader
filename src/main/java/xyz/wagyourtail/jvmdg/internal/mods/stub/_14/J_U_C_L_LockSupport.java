package xyz.wagyourtail.jvmdg.internal.mods.stub._14;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.locks.LockSupport;

public class J_U_C_L_LockSupport {

    @Stub(value = JavaVersion.VERSION_14, desc = "Ljava/util/concurrent/locks/LockSupport;")
    public static void setCurrentBlocker(Object blocker) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method m = LockSupport.class.getDeclaredMethod("setBlocker", Thread.class, Object.class);
        m.setAccessible(true);
        m.invoke(null, Thread.currentThread(), blocker);
    }
}
