package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.net.URLClassLoader;

@InitRewrite(ref = @Ref(value = "Ljava/net/URLClassLoader;", member = "<init>", desc = "(Ljava/lang/String;[Ljava/net/URL;Ljava/lang/ClassLoader;)V"), to = "([Ljava/net/URL;Ljava/lang/ClassLoader;)V", keep = {1, 2})
@InitRewrite(ref = @Ref(value = "Ljava/net/URLClassLoader;", member = "<init>", desc = "(Ljava/lang/String;[Ljava/net/URL;Ljava/lang/ClassLoader;Ljava/net/URLStreamHandlerFactory;)V"), to = "([Ljava/net/URL;Ljava/lang/ClassLoader;Ljava/net/URLStreamHandlerFactory;)V", keep = {1, 2, 3})
public class J_N_URLClassLoader {

    // URLClassLoader(String,...) -> URLClassLoader(...) then record the name
    @InitPost(target = "(Ljava/lang/String;[Ljava/net/URL;Ljava/lang/ClassLoader;)V", from = {0})
    @InitPost(target = "(Ljava/lang/String;[Ljava/net/URL;Ljava/lang/ClassLoader;Ljava/net/URLStreamHandlerFactory;)V", from = {0})
    public static void setName(URLClassLoader self, String name) {
        J_L_ClassLoader.setClassloaderName(name, self);
    }

}
