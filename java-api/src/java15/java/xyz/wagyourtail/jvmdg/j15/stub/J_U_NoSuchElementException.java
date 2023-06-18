package xyz.wagyourtail.jvmdg.j15.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.NoSuchElementException;

public class J_U_NoSuchElementException {

    @Stub(opcVers = Opcodes.V15, ref = @Ref(value = "Ljava/util/NoSuchElementException", member = "<init>"))
    public static NoSuchElementException create(String s, Throwable cause) throws NoSuchMethodException {
        var nse = new NoSuchElementException(s);
        nse.initCause(cause);
        return nse;
    }

    @Stub(opcVers = Opcodes.V15, ref = @Ref(value = "Ljava/util/NoSuchElementException", member = "<init>"))
    public static NoSuchElementException create(Throwable cause) throws NoSuchMethodException {
        var nse = new NoSuchElementException(cause == null ? null : cause.toString());
        nse.initCause(cause);
        return nse;
    }

}
