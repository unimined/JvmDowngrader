package xyz.wagyourtail.jvmdg.classloader.providers;

import xyz.wagyourtail.jvmdg.classloader.ResourceProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarFileResourceProvider implements ResourceProvider {
    private final JarFile jarFile;

    public JarFileResourceProvider(JarFile jarFile) {
        this.jarFile = jarFile;
    }


    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        final JarEntry entry = jarFile.getJarEntry(name);
        if (entry == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(Collections.singletonList(new URL("x-buffer", null, -1, name, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL u1) {
                return new URLConnection(u1) {
                    @Override
                    public void connect() {
                    }

                    @Override
                    public InputStream getInputStream() {
                        try {
                            return jarFile.getInputStream(entry);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        })));
    }

    @Override
    public void close() throws IOException {
        jarFile.close();
    }

}
