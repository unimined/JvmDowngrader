package xyz.wagyourtail.jvmdg.j19.stub.java_base;


import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.io.InvalidObjectException;

@InitRewrite(ref = @Ref(value = "Ljava/io/InvalidObjectException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"), to = "(Ljava/lang/String;)V", keep = {0})
public class J_I_InvalidObjectException {

    // InvalidObjectException(String,Throwable) -> InvalidObjectException(String) then initCause
    @InitPost(target = "(Ljava/lang/String;Ljava/lang/Throwable;)V")
    public static void setCause(InvalidObjectException e, Throwable t) {
        e.initCause(t);
    }

}
