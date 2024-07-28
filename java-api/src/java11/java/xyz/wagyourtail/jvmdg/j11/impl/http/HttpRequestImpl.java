package xyz.wagyourtail.jvmdg.j11.impl.http;

import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpClient;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpRequest;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HttpRequestImpl extends J_N_H_HttpRequest {
    URI uri;
    boolean expectContinue;
    J_N_H_HttpClient.Version version;
    Map<String, List<String>> headers = new HashMap<>();
    Duration timeout;
    String method;
    J_N_H_HttpRequest.BodyPublisher publisher;

    public HttpRequestImpl(HttpRequestBuilderImpl builder) {
        this.uri = builder.uri;
        this.expectContinue = builder.expectContinue;
        this.version = builder.version;
        // deep copy headers
        builder.headers.forEach((k, v) -> headers.put(k, List.copyOf(v)));
        this.timeout = builder.timeout;
        this.method = builder.method;
        this.publisher = builder.publisher;
    }

    @Override
    public Optional<BodyPublisher> bodyPublisher() {
        return Optional.ofNullable(publisher);
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public Optional<Duration> timeout() {
        return Optional.ofNullable(timeout);
    }

    @Override
    public boolean expectContinue() {
        return expectContinue;
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public Optional<J_N_H_HttpClient.Version> version() {
        return Optional.ofNullable(version);
    }
}
