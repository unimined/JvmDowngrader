package xyz.wagyourtail.jvmdg.standalone.classloader;

import xyz.wagyourtail.jvmdg.VersionProvider;
import xyz.wagyourtail.jvmdg.standalone.ClassDowngrader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class DowngradingClassLoader extends ClassLoader {
    private final List<ClassLoader> delegates = new ArrayList<>();
    private final ClassDowngrader downgrader = new ClassDowngrader(VersionProvider.getCurrentClassVersion());

    public DowngradingClassLoader() {
        super();
    }

    public DowngradingClassLoader(ClassLoader parent) {
        super(parent);
    }

    public DowngradingClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
        delegates.add(new URLClassLoader(urls, getParent()));
    }

    public DowngradingClassLoader(URL[] urls) {
        super();
        delegates.add(new URLClassLoader(urls, getParent()));
    }

    public void addDelegate(ClassLoader loader) {
        delegates.add(loader);
    }

    public void addDelegate(URL[] urls) {
        delegates.add(new URLClassLoader(urls, getParent()));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";
        URL url = findResource(path);
        if (url == null) {
            throw new ClassNotFoundException(name);
        }
        byte[] bytes;
        try {
            bytes = readAllBytes(url.openStream());
            Map<String, byte[]> extra = new HashMap<>();
            bytes = downgrader.downgrade(name, bytes, extra);
            for (Map.Entry<String, byte[]> entry : extra.entrySet()) {
                String extraName = entry.getKey();
                byte[] extraBytes = entry.getValue();
                defineClass(extraName, extraBytes, 0, extraBytes.length);
            }
        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        }
        try {
            return defineClass(name, bytes, 0, bytes.length, null);
        } catch (ClassFormatError e) {
            downgrader.writeBytesToDebug(name, bytes);
            throw e;
        }
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        int readBytes = 0;
        byte[] bytes = new byte[8192];
        // read into bytes
        int read;
        while ((read = in.read(bytes, readBytes, bytes.length - readBytes)) != -1) {
            readBytes += read;
            if (readBytes == bytes.length) {
                byte[] old = bytes;
                bytes = new byte[readBytes << 1];
                System.arraycopy(old, 0, bytes, 0, readBytes);
            }
        }
        if (readBytes == bytes.length) return bytes;
        byte[] trimmed = new byte[readBytes];
        System.arraycopy(bytes, 0, trimmed, 0, readBytes);
        return trimmed;
    }

    @Override
    protected URL findResource(String name) {
        for (ClassLoader delegate : delegates) {
            URL resource = delegate.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Vector<URL> vector = new Vector<>();
        for (ClassLoader delegate : delegates) {
            Enumeration<URL> enumeration = delegate.getResources(name);
            while (enumeration.hasMoreElements()) {
                vector.add(enumeration.nextElement());
            }
        }
        return vector.elements();
    }

}
