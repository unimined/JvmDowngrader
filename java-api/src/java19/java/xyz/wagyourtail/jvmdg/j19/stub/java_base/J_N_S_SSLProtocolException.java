package xyz.wagyourtail.jvmdg.j19.stub.java_base;


import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import javax.net.ssl.SSLProtocolException;

@InitRewrite(ref = @Ref(value = "Ljavax/net/ssl/SSLProtocolException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"), to = "(Ljava/lang/String;)V", keep = {0})
public class J_N_S_SSLProtocolException {

    // SSLProtocolException(String,Throwable) -> SSLProtocolException(String) then initCause
    @InitPost(target = "(Ljava/lang/String;Ljava/lang/Throwable;)V")
    public static void setCause(SSLProtocolException e, Throwable t) {
        e.initCause(t);
    }

}
