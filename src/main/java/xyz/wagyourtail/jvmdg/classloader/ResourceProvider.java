package xyz.wagyourtail.jvmdg.classloader;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public interface ResourceProvider extends Closeable {

    Enumeration<URL> getResources(String name) throws IOException;

}
