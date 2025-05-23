package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;

public class J_N_Authenticator {

    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle getTheAuthenticator;

    static {
        try {
            getTheAuthenticator = IMPL_LOOKUP.findStaticGetter(Authenticator.class, "theAuthenticator", Authenticator.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Stub(ref = @Ref("Ljava/net/Authenticator;"))
    public static Authenticator getDefault() throws Throwable {
        return (Authenticator) getTheAuthenticator.invokeExact();
    }

    @Stub(ref = @Ref("Ljava/net/Authenticator;"))
    public static PasswordAuthentication requestPasswordAuthentication(
        Authenticator authenticator,
        String host,
        InetAddress addr,
        int port,
        String protocol,
        String prompt,
        String scheme,
        URL url,
        Authenticator.RequestorType reqType
    ) throws Throwable {
        return requestPasswordAuthenticationInstance(authenticator, host, addr, port, protocol, prompt, scheme, url, reqType);
    }

    @Stub
    public static PasswordAuthentication requestPasswordAuthenticationInstance(
        Authenticator authenticator,
        String host,
        InetAddress addr,
        int port,
        String protocol,
        String prompt,
        String scheme,
        URL url,
        Authenticator.RequestorType reqType
    ) throws Throwable {
        synchronized (authenticator) {
            Authenticator oldValue = getDefault();

            try {
                Authenticator.setDefault(authenticator);
                return Authenticator.requestPasswordAuthentication(
                    host,
                    addr,
                    port,
                    protocol,
                    prompt,
                    scheme,
                    url,
                    reqType
                );
            } finally {
                Authenticator.setDefault(oldValue);
            }
        }
    }



}
