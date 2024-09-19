package xyz.wagyourtail.jvmdg.j11.impl.http;

import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpClient;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpHeaders;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpResponse;

public class HttpResponseInfo implements J_N_H_HttpResponse.ResponseInfo {
    int statusCode;
    J_N_H_HttpHeaders headers;
    J_N_H_HttpClient.Version version;

    public HttpResponseInfo(int statusCode, J_N_H_HttpHeaders headers, J_N_H_HttpClient.Version version) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.version = version;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public J_N_H_HttpHeaders headers() {
        return headers;
    }

    @Override
    public J_N_H_HttpClient.Version version() {
        return version;
    }

}
