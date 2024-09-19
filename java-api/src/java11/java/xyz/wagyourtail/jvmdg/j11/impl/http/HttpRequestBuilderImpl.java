package xyz.wagyourtail.jvmdg.j11.impl.http;

import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpClient;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpRequest;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestBuilderImpl implements J_N_H_HttpRequest.Builder {
    URI uri;
    boolean expectContinue = false;
    J_N_H_HttpClient.Version version;
    Map<String, List<String>> headers = new HashMap<>();
    Duration timeout;
    String method = "GET";
    J_N_H_HttpRequest.BodyPublisher publisher;

    @Override
    public J_N_H_HttpRequest.Builder uri(URI uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public J_N_H_HttpRequest.Builder expectContinue(boolean expectContinue) {
        this.expectContinue = expectContinue;
        return this;
    }

    @Override
    public J_N_H_HttpRequest.Builder version(J_N_H_HttpClient.Version version) {
        this.version = version;
        return this;
    }

    @Override
    public J_N_H_HttpRequest.Builder header(String name, String value) {
        if (!headers.containsKey(name)) {
            headers.put(name, new ArrayList<>(List.of(value)));
        } else {
            headers.get(name).add(value);
        }
        return this;
    }

    @Override
    public J_N_H_HttpRequest.Builder headers(String... headers) {
        for (int i = 0; i < headers.length; i += 2) {
            header(headers[i], headers[i + 1]);
        }
        return this;
    }

    @Override
    public J_N_H_HttpRequest.Builder timeout(Duration duration) {
        this.timeout = duration;
        return this;
    }

    @Override
    public J_N_H_HttpRequest.Builder setHeader(String name, String value) {
        headers.put(name, new ArrayList<>(List.of(value)));
        return this;
    }

    @Override
    public J_N_H_HttpRequest.Builder GET() {
        return method("GET", null);
    }

    @Override
    public J_N_H_HttpRequest.Builder POST(J_N_H_HttpRequest.BodyPublisher publisher) {
        return method("POST", publisher);
    }

    @Override
    public J_N_H_HttpRequest.Builder PUT(J_N_H_HttpRequest.BodyPublisher publisher) {
        return method("PUT", publisher);
    }

    @Override
    public J_N_H_HttpRequest.Builder DELETE() {
        return method("DELETE", null);
    }

    @Override
    public J_N_H_HttpRequest.Builder method(String name, J_N_H_HttpRequest.BodyPublisher publisher) {
        this.method = name;
        this.publisher = publisher;
        return this;
    }

    @Override
    public J_N_H_HttpRequest build() {
        return new HttpRequestImpl(this);
    }

    @Override
    public J_N_H_HttpRequest.Builder copy() {
        HttpRequestBuilderImpl copy = new HttpRequestBuilderImpl();
        copy.uri = uri;
        copy.expectContinue = expectContinue;
        copy.version = version;
        // deep copy headers
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            copy.headers.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        copy.timeout = timeout;
        copy.method = method;
        copy.publisher = publisher;
        return copy;
    }

}
