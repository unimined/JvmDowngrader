package xyz.wagyourtail.jvmdg.j11.stub.java_net_http;


import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;

public class HttpClientBuilderImpl implements J_N_H_HttpClient.Builder {
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


    @Override
    public J_N_H_HttpClient.Builder cookieHandler(CookieHandler handler) {
        Objects.requireNonNull(handler);
        this.cookieHandler = handler;
        return this;
    }

    @Override
    public J_N_H_HttpClient.Builder connectTimeout(Duration duration) {
        Objects.requireNonNull(duration);
        if (duration.isNegative() || Duration.ZERO.equals(duration)) {
            throw new IllegalArgumentException("Invalid duration: " + duration);
        }
        this.connectTimeout = duration;
        return this;
    }

    @Override
    public J_N_H_HttpClient.Builder sslContext(SSLContext context) {
        Objects.requireNonNull(context);
        this.sslContext = context;
        return this;
    }

    @Override
    public J_N_H_HttpClient.Builder sslParameters(SSLParameters parameters) {
        Objects.requireNonNull(parameters);
        this.sslParams = parameters;
        return this;
    }

    @Override
    public J_N_H_HttpClient.Builder executor(Executor executor) {
        Objects.requireNonNull(executor);
        this.executor = executor;
        return this;
    }

    @Override
    public J_N_H_HttpClient.Builder followRedirects(J_N_H_HttpClient.Redirect policy) {
        Objects.requireNonNull(policy);
        this.followRedirects = policy;
        return this;
    }

    @Override
    public J_N_H_HttpClient.Builder version(J_N_H_HttpClient.Version version) {
        Objects.requireNonNull(version);
        this.version = version;
        return this;
    }

    @Override
    public J_N_H_HttpClient.Builder priority(int priority) {
        if (priority < 1 || priority > 256) {
            throw new IllegalArgumentException("Invalid priority: " + priority);
        }
        this.priority = priority;
        return this;
    }

    @Override
    public J_N_H_HttpClient.Builder proxy(ProxySelector selector) {
        Objects.requireNonNull(selector);
        this.proxy = selector;
        return this;
    }

    @Override
    public J_N_H_HttpClient.Builder authenticator(Authenticator authenticator) {
        Objects.requireNonNull(authenticator);
        this.authenticator = authenticator;
        return this;
    }

    @Override
    public J_N_H_HttpClient build() {
        return new HttpClientImpl(this);
    }


}
