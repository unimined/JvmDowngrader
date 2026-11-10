package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;

/** Hand-written parity tests for members the generator cannot synthesize. */
public final class NetTests {

    private NetTests() {
    }

    /** URLStreamHandler is an abstract class — no lambda or alias synthesis. */
    @StubTest(member = "java/net/URL.of(Ljava/net/URI;Ljava/net/URLStreamHandler;)Ljava/net/URL;")
    public static void urlOf(String id) {
        try {
            java.net.URL u = java.net.URL.of(java.net.URI.create("http://localhost/a"),
                new java.net.URLStreamHandler() {
                    @Override
                    protected java.net.URLConnection openConnection(java.net.URL u2) {
                        return null;
                    }
                });
            Support.print(id, u);
        } catch (Throwable t) {
            Support.exc(id, t);
        }
    }

    /** [modify]-kind ctor whose factory arg is unsynthesizable. */
    @StubTest(member = "java/net/URLClassLoader.<init>(Ljava/lang/String;[Ljava/net/URL;"
        + "Ljava/lang/ClassLoader;Ljava/net/URLStreamHandlerFactory;)V")
    public static void urlClassLoader(String id) {
        try {
            java.net.URLClassLoader cl = new java.net.URLClassLoader("x", new java.net.URL[0],
                String.class.getClassLoader(), (java.net.URLStreamHandlerFactory) p -> null);
            Support.print(id, java.util.Arrays.toString(cl.getURLs()));
            cl.close();
        } catch (Throwable t) {
            Support.exc(id, t);
        }
    }

    /** HttpResponse has no synthesizable receiver — fabricate one anonymously. */
    @StubTest(member = "java/net/http/HttpResponse.connectionLabel()Ljava/util/Optional;")
    public static void connectionLabel(String id) {
        try {
            java.net.http.HttpResponse<Object> r = new java.net.http.HttpResponse<Object>() {
                @Override public int statusCode() { return 200; }
                @Override public java.net.http.HttpRequest request() { return null; }
                @Override public java.util.Optional<java.net.http.HttpResponse<Object>> previousResponse() {
                    return java.util.Optional.empty();
                }
                @Override public java.net.http.HttpHeaders headers() { return null; }
                @Override public Object body() { return null; }
                @Override public java.util.Optional<javax.net.ssl.SSLSession> sslSession() {
                    return java.util.Optional.empty();
                }
                @Override public java.net.URI uri() { return null; }
                @Override public java.net.http.HttpClient.Version version() {
                    return java.net.http.HttpClient.Version.HTTP_1_1;
                }
            };
            Support.print(id, r.connectionLabel());
        } catch (Throwable t) {
            Support.exc(id, t);
        }
    }
}
