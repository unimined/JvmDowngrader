package xyz.wagyourtail.jvmdg.j11.impl.http;

import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpClient;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpHeaders;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpRequest;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpResponse;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.util.Optional;

public class HttpResponseImpl<T> implements J_N_H_HttpResponse<T> {
    J_N_H_HttpRequest request;
    HttpResponseInfo info;
    T body;
    SSLSession sslSession;

    public HttpResponseImpl(J_N_H_HttpRequest request, HttpResponseInfo info, T body, SSLSession sslSession) {
        this.request = request;
        this.info = info;
        this.body = body;
        this.sslSession = sslSession;
    }

    @Override
    public int statusCode() {
        return info.statusCode();
    }

    @Override
    public J_N_H_HttpRequest request() {
        return request;
    }

    @Override
    public Optional<J_N_H_HttpResponse<T>> previousResponse() {
        return Optional.empty();
    }

    @Override
    public J_N_H_HttpHeaders headers() {
        return info.headers();
    }

    @Override
    public T body() {
        return body;
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.ofNullable(sslSession);
    }

    @Override
    public URI uri() {
        return request.uri();
    }

    @Override
    public J_N_H_HttpClient.Version version() {
        return info.version();
    }

}
