package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;

public class J_N_Authenticator {

    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle getTheAuthenticator;

    private static final MethodHandle setRequestingHost;
    private static final MethodHandle setRequestingSite;
    private static final MethodHandle setRequestingPort;
    private static final MethodHandle setRequestingProtocol;
    private static final MethodHandle setRequestingPrompt;
    private static final MethodHandle setRequestingScheme;
    private static final MethodHandle setRequestingURL;
    private static final MethodHandle setRequestingAuthType;

    private static final MethodHandle getPasswordAuthentication;

    static {
        try {
            getTheAuthenticator = IMPL_LOOKUP.findStaticGetter(Authenticator.class, "theAuthenticator", Authenticator.class);

            setRequestingHost = IMPL_LOOKUP.findSetter(Authenticator.class, "requestingHost", String.class);
            setRequestingSite = IMPL_LOOKUP.findSetter(Authenticator.class, "requestingSite", InetAddress.class);
            setRequestingPort = IMPL_LOOKUP.findSetter(Authenticator.class, "requestingPort", int.class);
            setRequestingProtocol = IMPL_LOOKUP.findSetter(Authenticator.class, "requestingProtocol", String.class);
            setRequestingPrompt = IMPL_LOOKUP.findSetter(Authenticator.class, "requestingPrompt", String.class);
            setRequestingScheme = IMPL_LOOKUP.findSetter(Authenticator.class, "requestingScheme", String.class);
            setRequestingURL = IMPL_LOOKUP.findSetter(Authenticator.class, "requestingURL", URL.class);
            setRequestingAuthType = IMPL_LOOKUP.findSetter(Authenticator.class, "requestingAuthType", Authenticator.RequestorType.class);

            getPasswordAuthentication = IMPL_LOOKUP.findVirtual(Authenticator.class, "getPasswordAuthentication", MethodType.methodType(PasswordAuthentication.class));
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
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
        if (authenticator == null) {
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
        }
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
            setRequestingHost.invokeExact(authenticator, host);
            setRequestingSite.invokeExact(authenticator, addr);
            setRequestingPort.invokeExact(authenticator, port);
            setRequestingProtocol.invokeExact(authenticator, protocol);
            setRequestingPrompt.invokeExact(authenticator, prompt);
            setRequestingScheme.invokeExact(authenticator, scheme);
            setRequestingURL.invokeExact(authenticator, url);
            setRequestingAuthType.invokeExact(authenticator, reqType);

            return (PasswordAuthentication) getPasswordAuthentication.invokeExact(authenticator);
        }
    }



}
