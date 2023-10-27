package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_InternalError {

    @Stub(ref = @Ref(value = "java/lang/InternalError", member = "<init>"))
    public static InternalError init(String message, Throwable cause) {
        InternalError ie = new InternalError(message);
        ie.initCause(cause);
        return ie;
    }

    @Stub(ref = @Ref(value = "java/lang/InternalError", member = "<init>"))
    public static InternalError init(Throwable cause) {
        InternalError ie = new InternalError();
        ie.initCause(cause);
        return ie;
    }

}