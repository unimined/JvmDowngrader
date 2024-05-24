package xyz.wagyourtail.jvmdg.classloader.providers;

import xyz.wagyourtail.jvmdg.classloader.ResourceProvider;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class ClassLoaderResourceProvider implements ResourceProvider {
    private final ClassLoader classLoader;

    public ClassLoaderResourceProvider(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return classLoader.getResources(name);
    }

    @Override
    public void close() throws IOException {
        if (classLoader instanceof Closeable) {
            ((Closeable) classLoader).close();
        } else if (classLoader instanceof AutoCloseable) {
            try {
                ((AutoCloseable) classLoader).close();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

}
