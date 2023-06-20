package xyz.wagyourtail.jvmdg.j11.stub.httpclient;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;

@Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/net/http/HttpClient;"))
public abstract class J_N_H_HttpClient {


    public static J_N_H_HttpClient newHttpClient() {
        return new HttpClientBuilderImpl().build();
    }

    public static Builder newBuilder() {
        return new HttpClientBuilderImpl();
    }

    public abstract Optional<CookieHandler> cookieHandler();

    public abstract Optional<Duration> connectTimeout();

    public abstract Redirect followRedirects();

    public abstract Optional<ProxySelector> proxy();

    public abstract SSLContext sslContext();

    public abstract SSLParameters sslParameters();

    public abstract Optional<Authenticator> authenticator();

    public abstract Version version();

    public abstract Optional<Executor> executor();

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/net/http/HttpClient$Redirect;"))
    public enum Redirect {
        NEVER,
        ALWAYS,
        NORMAL
    }

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/net/http/HttpClient$Version;"))
    public enum Version {
        HTTP_1_1,
        HTTP_2
    }

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/net/http/HttpClient$Builder;"))
    public interface Builder {
        Builder cookieHandler(CookieHandler handler);

        Builder connectTimeout(Duration duration);

        Builder sslContext(SSLContext context);

        Builder sslParameters(SSLParameters parameters);

        Builder executor(Executor executor);

        Builder followRedirects(Redirect policy);

        Builder version(Version version);

        Builder priority(int priority);

        Builder proxy(ProxySelector selector);

        Builder authenticator(Authenticator authenticator);

        J_N_H_HttpClient build();

    }

}
