package xyz.wagyourtail.jvmdg.j19.stub.java_base;


import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.io.InvalidClassException;

@InitRewrite(ref = @Ref(value = "Ljava/io/InvalidClassException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V"), to = "(Ljava/lang/String;)V", keep = {0})
@InitRewrite(ref = @Ref(value = "Ljava/io/InvalidClassException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"), to = "(Ljava/lang/String;)V", keep = {0})
public class J_I_InvalidClassException {

    // InvalidClassException(String reason, String cname, Throwable) -> <init>(reason), set classname, initCause
    @InitPost(target = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V", from = {1, 2})
    public static void setNameAndCause(InvalidClassException e, String classname, Throwable t) {
        e.classname = classname;
        e.initCause(t);
    }

    // InvalidClassException(String,Throwable) -> InvalidClassException(String) then initCause
    @InitPost(target = "(Ljava/lang/String;Ljava/lang/Throwable;)V")
    public static void setCause(InvalidClassException e, Throwable t) {
        e.initCause(t);
    }

}
