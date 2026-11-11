package xyz.wagyourtail.jvmdg.j19.stub.java_base;


import xyz.wagyourtail.jvmdg.j8.intl.ExceptionHelper;
import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.io.ObjectStreamException;

@InitRewrite(ref = @Ref(value = "Ljava/io/ObjectStreamException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"), to = "(Ljava/lang/String;)V", keep = {0})
@InitRewrite(ref = @Ref(value = "Ljava/io/ObjectStreamException;", member = "<init>", desc = "(Ljava/lang/Throwable;)V"), to = "(Ljava/lang/String;)V", keep = {-1})
public class J_I_ObjectStreamException {

    // ObjectStreamException(String,Throwable) / (Throwable) -> ObjectStreamException(String) then initCause
    @InitPost(target = "(Ljava/lang/String;Ljava/lang/Throwable;)V")
    @InitPost(target = "(Ljava/lang/Throwable;)V")
    public static void setCause(ObjectStreamException e, Throwable t) {
        e.initCause(t);
    }

    @InitArg(target = "(Ljava/lang/Throwable;)V", slot = 0)
    public static String msg(Throwable t) {
        return ExceptionHelper.causeMessage(t);
    }

}
