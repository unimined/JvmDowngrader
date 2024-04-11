package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

@Adapter("java/util/concurrent/CompletionException")
public class J_U_C_CompletionException extends RuntimeException {

    protected J_U_C_CompletionException() {
        super();
    }

    protected J_U_C_CompletionException(String message) {
        super(message);
    }

    public J_U_C_CompletionException(String message, Throwable cause) {
        super(message, cause);
    }

    public J_U_C_CompletionException(Throwable cause) {
        super(cause);
    }

}