package xyz.wagyourtail.jvmdg.j11.stub.java_net_http;


import xyz.wagyourtail.jvmdg.j11.impl.http.HttpClientBuilderImpl;
import xyz.wagyourtail.jvmdg.version.Adapter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * HttpClient is a bit incomplete.
 * the current implementation uses {@link java.net.URL#openConnection()}
 *
 * <p>
 * the following attributes are <i>currently</i> a no-op
 * {@link #version()},
 * {@link J_N_H_HttpRequest#expectContinue()},
 * {@link #authenticator()}
 * <p>
 * the following attributes haven't been tested for correctness
 * {@link #proxy()},
 * {@link #followRedirects()},
 * {@link #cookieHandler()},
 * {@link #sslParameters()}
 * <p>
 * Body publishers and all the {@link java.util.concurrent.Flow} implementations also haven't been tested completely.
 * If you encounter any issues, please open a bug report.
 *
 * @see xyz.wagyourtail.jvmdg.j11.impl.http.HttpClientImpl
 */
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

    public abstract <T> J_N_H_HttpResponse<T> send(J_N_H_HttpRequest var1, J_N_H_HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException;

    public abstract <T>CompletableFuture<J_N_H_HttpResponse<T>> sendAsync(J_N_H_HttpRequest var1, J_N_H_HttpResponse.BodyHandler<T> handler);

    public abstract <T> CompletableFuture<J_N_H_HttpResponse<T>> sendAsync(J_N_H_HttpRequest var1, J_N_H_HttpResponse.BodyHandler<T> handler, J_N_H_HttpResponse.PushPromiseHandler<T> pushPromiseHandler);

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
