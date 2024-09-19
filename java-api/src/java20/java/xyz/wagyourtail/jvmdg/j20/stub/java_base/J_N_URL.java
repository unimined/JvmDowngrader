package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.net.*;

public class J_N_URL {

    static boolean isOverrideable(String protocol) {
        if (protocol.length() == 3) {
            return (Character.toLowerCase(protocol.charAt(0)) != 'j') ||
                (Character.toLowerCase(protocol.charAt(1)) != 'r') ||
                (Character.toLowerCase(protocol.charAt(2)) != 't');
        } else if (protocol.length() == 4) {
            return (Character.toLowerCase(protocol.charAt(0)) != 'f') ||
                (Character.toLowerCase(protocol.charAt(1)) != 'i') ||
                (Character.toLowerCase(protocol.charAt(2)) != 'l') ||
                (Character.toLowerCase(protocol.charAt(3)) != 'e');
        }
        return true;
    }

    @Stub(ref = @Ref("java/net/URL"))
    public static URL of(URI uri, URLStreamHandler handler) throws MalformedURLException {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }

        String protocol = uri.getScheme();
        if (handler == null && protocol.equals("jrt") && !uri.isOpaque() && uri.getRawAuthority() == null && uri.getRawFragment() == null) {
            String query = uri.getRawQuery();
            String path = uri.getRawPath();
            String file = (query == null) ? path : path + "?" + query;

            String host = uri.getHost();
            if (host == null) {
                host = "";
            }

            int port = uri.getPort();
            return new URL(protocol, host, port, file, null);
        }

        if ("url".equalsIgnoreCase(protocol)) {
            String uriStr = uri.toString();
            try {
                URI inner = new URI(uriStr.substring(4));
                if (inner.isAbsolute()) {
                    protocol = inner.getScheme();
                }
            } catch (URISyntaxException e) {
                throw new MalformedURLException(e.getMessage());
            }
        }

        if (handler != null && !isOverrideable(protocol)) {
            throw new IllegalArgumentException("Can't override URLStreamHandler for protocol " + protocol);
        }

        return new URL(null, uri.toString(), handler);
    }

}
