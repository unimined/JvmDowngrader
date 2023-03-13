package xyz.wagyourtail.jvmdg.j14.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.locks.LockSupport;

public class J_U_C_L_LockSupport {

    @Stub(javaVersion = Opcodes.V14, ref = @Ref("Ljava/util/concurrent/locks/LockSupport;"))
    public static void setCurrentBlocker(Object blocker) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method m = LockSupport.class.getDeclaredMethod("setBlocker", Thread.class, Object.class);
        m.setAccessible(true);
        m.invoke(null, Thread.currentThread(), blocker);
    }

}
