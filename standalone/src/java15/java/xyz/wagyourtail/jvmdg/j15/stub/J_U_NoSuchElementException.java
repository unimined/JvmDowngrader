package xyz.wagyourtail.jvmdg.j15.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.lang.reflect.Method;
import java.util.NoSuchElementException;

public class J_U_NoSuchElementException {

    @Stub(javaVersion = Opcodes.V15, ref = @Ref(value = "Ljava/util/NoSuchElementException", member = "<init>"))
    public static NoSuchElementException create(String s, Throwable cause) throws NoSuchMethodException {
        var nse = new NoSuchElementException(s);
        // call setCause
        Method setCause = Throwable.class.getDeclaredMethod("setCause", Throwable.class);
        setCause.setAccessible(true);
        try {
            setCause.invoke(nse, cause);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nse;
    }

    @Stub(javaVersion = Opcodes.V15, ref = @Ref(value = "Ljava/util/NoSuchElementException", member = "<init>"))
    public static NoSuchElementException create(Throwable cause) throws NoSuchMethodException {
        var nse = new NoSuchElementException(cause == null ? null : cause.toString());
        // call setCause
        Method setCause = Throwable.class.getDeclaredMethod("setCause", Throwable.class);
        setCause.setAccessible(true);
        try {
            setCause.invoke(nse, cause);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nse;
    }

}
