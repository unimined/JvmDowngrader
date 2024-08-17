package xyz.wagyourtail.jvmdg.j15.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class J_L_I_ConstantBootstraps {

    @Stub(ref = @Ref("java/lang/invoke/ConstantBootstraps"))
    public static Object explicitCast(MethodHandles.Lookup lookup, String name, Class<?> dstType, Object value) throws ClassCastException {
        if (dstType == void.class)
            throw new ClassCastException("Can not convert to void");
        if (dstType == Object.class)
            return value;

        MethodHandle id = MethodHandles.identity(dstType);
        MethodType mt = MethodType.methodType(dstType, Object.class);
        MethodHandle conv = MethodHandles.explicitCastArguments(id, mt);
        try {
            return conv.invoke(value);
        } catch (ClassCastException e) {
            throw e; // specified, let CCE through
        } catch (Throwable e) {
            ClassCastException ex = new ClassCastException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

}
