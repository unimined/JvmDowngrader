package xyz.wagyourtail.jvmdg.j20.stub.java_base;


import xyz.wagyourtail.jvmdg.j8.intl.ExceptionHelper;
import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.security.InvalidParameterException;

@InitRewrite(ref = @Ref(value = "Ljava/security/InvalidParameterException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"), to = "(Ljava/lang/String;)V", keep = {0})
@InitRewrite(ref = @Ref(value = "Ljava/security/InvalidParameterException;", member = "<init>", desc = "(Ljava/lang/Throwable;)V"), to = "(Ljava/lang/String;)V", keep = {-1})
public class J_S_InvalidParameterException {

    // InvalidParameterException(String,Throwable) / (Throwable) -> InvalidParameterException(String) then initCause
    @InitPost(target = "(Ljava/lang/String;Ljava/lang/Throwable;)V")
    @InitPost(target = "(Ljava/lang/Throwable;)V")
    public static void setCause(InvalidParameterException e, Throwable t) {
        e.initCause(t);
    }

    @InitArg(target = "(Ljava/lang/Throwable;)V", slot = 0)
    public static String msg(Throwable t) {
        return ExceptionHelper.causeMessage(t);
    }

}
