package xyz.wagyourtail.jvmdg.j8.stub;


import xyz.wagyourtail.jvmdg.j8.intl.ExceptionHelper;
import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitPosts;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.InitRewrites;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.lang.VirtualMachineError;

// explicit containers — this source set compiles below Java 8
@InitRewrites({
    @InitRewrite(ref = @Ref(value = "Ljava/lang/VirtualMachineError;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"), to = "(Ljava/lang/String;)V", keep = {0}),
    @InitRewrite(ref = @Ref(value = "Ljava/lang/VirtualMachineError;", member = "<init>", desc = "(Ljava/lang/Throwable;)V"), to = "(Ljava/lang/String;)V", keep = {-1})
})
public class J_L_VirtualMachineError {

    // VirtualMachineError(String,Throwable) / (Throwable) -> VirtualMachineError(String) then initCause
    @InitPosts({
        @InitPost(target = "(Ljava/lang/String;Ljava/lang/Throwable;)V"),
        @InitPost(target = "(Ljava/lang/Throwable;)V")
    })
    public static void setCause(VirtualMachineError e, Throwable t) {
        e.initCause(t);
    }

    @InitArg(target = "(Ljava/lang/Throwable;)V", slot = 0)
    public static String msg(Throwable t) {
        return ExceptionHelper.causeMessage(t);
    }

}
