package xyz.wagyourtail.jvmdg.j15.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.NoSuchElementException;

public class J_U_NoSuchElementException {

    @Stub(ref = @Ref(value = "Ljava/util/NoSuchElementException;", member = "<init>"))
    public static NoSuchElementException create(String s, Throwable cause) throws NoSuchMethodException {
        var nse = new NoSuchElementException(s);
        nse.initCause(cause);
        return nse;
    }

    @Stub(ref = @Ref(value = "Ljava/util/NoSuchElementException;", member = "<init>"))
    public static NoSuchElementException create(Throwable cause) throws NoSuchMethodException {
        var nse = new NoSuchElementException(cause == null ? null : cause.toString());
        nse.initCause(cause);
        return nse;
    }

}
