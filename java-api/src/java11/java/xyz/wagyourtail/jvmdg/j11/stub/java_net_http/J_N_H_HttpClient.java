package xyz.wagyourtail.jvmdg.j11.stub.java_net_http;


import xyz.wagyourtail.jvmdg.j11.impl.HttpClientBuilderImpl;
import xyz.wagyourtail.jvmdg.version.Adapter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;

@Adapter("Ljava/net/http/HttpClient;")
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

    @Adapter("Ljava/net/http/HttpClient$Redirect;")
    public enum Redirect {
        NEVER,
        ALWAYS,
        NORMAL
    }

    @Adapter("Ljava/net/http/HttpClient$Version;")
    public enum Version {
        HTTP_1_1,
        HTTP_2
    }

    @Adapter("Ljava/net/http/HttpClient$Builder;")
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
