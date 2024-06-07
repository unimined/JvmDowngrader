package xyz.wagyourtail.jvmdg.j11.impl.http;

import jdk.incubator.http.HttpRequest;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpClient;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpResponse;

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
    public <T> J_N_H_HttpResponse<T> send(HttpRequest var1, J_N_H_HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        throw MissingStubError.create();
    }

    @Override
    public <T> CompletableFuture<J_N_H_HttpResponse<T>> sendAsync(HttpRequest var1, J_N_H_HttpResponse.BodyHandler<T> handler) {
        throw MissingStubError.create();
    }

    @Override
    public <T> CompletableFuture<J_N_H_HttpResponse<T>> sendAsync(HttpRequest var1, J_N_H_HttpResponse.BodyHandler<T> handler, J_N_H_HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        throw MissingStubError.create();
    }

}
