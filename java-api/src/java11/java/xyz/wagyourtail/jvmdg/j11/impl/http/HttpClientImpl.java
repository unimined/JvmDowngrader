package xyz.wagyourtail.jvmdg.j11.impl.http;

import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpClient;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpHeaders;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpRequest;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpResponse;
import xyz.wagyourtail.jvmdg.util.Utils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;


public class HttpClientImpl extends J_N_H_HttpClient {
    CookieHandler cookieHandler;
    Duration connectTimeout;
    J_N_H_HttpClient.Redirect followRedirects;
    ProxySelector proxy;
    Authenticator authenticator;
    J_N_H_HttpClient.Version version;
    Executor executor;
    // Security parameters
    SSLContext sslContext;
    SSLParameters sslParams;
    int priority = -1;

    public HttpClientImpl(HttpClientBuilderImpl builder) {
        this.cookieHandler = builder.cookieHandler;
        this.connectTimeout = builder.connectTimeout;
        this.followRedirects = builder.followRedirects;
        this.proxy = builder.proxy;
        this.authenticator = builder.authenticator;
        this.version = builder.version;
        this.executor = builder.executor;
        this.sslContext = builder.sslContext;
        this.sslParams = builder.sslParams;
        this.priority = builder.priority;
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return Optional.ofNullable(cookieHandler);
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.ofNullable(connectTimeout);
    }

    @Override
    public Redirect followRedirects() {
        return followRedirects;
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return Optional.ofNullable(proxy);
    }

    @Override
    public SSLContext sslContext() {
        return sslContext;
    }

    @Override
    public SSLParameters sslParameters() {
        return sslParams;
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.ofNullable(authenticator);
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public Optional<Executor> executor() {
        return Optional.ofNullable(executor);
    }

    @Override
    public <T> J_N_H_HttpResponse<T> send(J_N_H_HttpRequest var1, J_N_H_HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        return sendImpl(var1, handler, null);
    }

    protected <T> J_N_H_HttpResponse<T> sendImpl(J_N_H_HttpRequest var1, J_N_H_HttpResponse.BodyHandler<T> handler, J_N_H_HttpResponse.PushPromiseHandler<T> pushPromiseHandler) throws IOException, InterruptedException {
        HttpURLConnection connection;
        Objects.requireNonNull(var1);
        Objects.requireNonNull(handler);

        if (var1.method().equals("CONNECT")) {
            throw new IllegalArgumentException("Unsupported method CONNECT");
        }

        if (proxy != null) {
            Proxy p = proxy.select(var1.uri()).stream().findFirst().orElse(Proxy.NO_PROXY);
            connection = (HttpURLConnection) var1.uri().toURL().openConnection(p);
        } else {
            connection = (HttpURLConnection) var1.uri().toURL().openConnection();
        }

        connection.setRequestMethod(var1.method());
        connection.setInstanceFollowRedirects(followRedirects != Redirect.NEVER);
        if (connectTimeout != null) {
            connection.setConnectTimeout((int) connectTimeout.toMillis());
        }
        var1.timeout().ifPresent(t -> connection.setReadTimeout((int) t.toMillis()));
        connection.setDoOutput(var1.bodyPublisher().isPresent());
        connection.setDoInput(handler != J_N_H_HttpResponse.BodyHandlers.discarding());
        connection.setAllowUserInteraction(false);

        HttpRequestImpl request = (HttpRequestImpl) var1;

        request.headers.forEach((k, v) -> connection.setRequestProperty(k, String.join(",", v)));

        J_N_H_HttpRequest.BodyPublisher publisher = request.publisher;

        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
            if (sslContext != null) {
                httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            }
        }

        connection.connect();

        if (publisher != null) {
            OutputStream out = connection.getOutputStream();
            publisher.subscribe(new Flow.Subscriber<>() {


                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    try {
                        out.write(item.array());
                    } catch (IOException e) {
                        Utils.sneakyThrow(e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.addSuppressed(throwable);
                        Utils.sneakyThrow(e);
                    }
                    Utils.sneakyThrow(throwable);
                }

                @Override
                public void onComplete() {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Utils.sneakyThrow(e);
                    }
                }

            });
        }

        int responseCode = connection.getResponseCode();
        Map<String, List<String>> headers = connection.getHeaderFields();
        Version version = J_N_H_HttpClient.Version.HTTP_1_1;
        HttpResponseInfo info = new HttpResponseInfo(responseCode, new J_N_H_HttpHeaders(headers), version);
        J_N_H_HttpResponse.BodySubscriber<T> subscriber = handler.apply(info);
        subscriber.onNext(List.of(ByteBuffer.wrap(connection.getInputStream().readAllBytes())));
        subscriber.onComplete();
        T body = subscriber.getBody().toCompletableFuture().join();

        return new HttpResponseImpl<>(var1, info, body, null);
    }

    @Override
    public <T> CompletableFuture<J_N_H_HttpResponse<T>> sendAsync(J_N_H_HttpRequest var1, J_N_H_HttpResponse.BodyHandler<T> handler) {
        return sendAsync(var1, handler, null);
    }

    @Override
    public <T> CompletableFuture<J_N_H_HttpResponse<T>> sendAsync(J_N_H_HttpRequest var1, J_N_H_HttpResponse.BodyHandler<T> handler, J_N_H_HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendImpl(var1, handler, pushPromiseHandler);
            } catch (IOException | InterruptedException e) {
                Utils.sneakyThrow(e);
            }
            return null;
        }, executor == null ? ForkJoinPool.commonPool() : executor);
    }

}
