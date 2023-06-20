package xyz.wagyourtail.jvmdg.classloader;

import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class DowngradingClassLoader extends ClassLoader {
    private final List<ClassLoader> delegates = new ArrayList<>();

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
        String internalName = name.replace('.', '/');
        String path = internalName + ".class";
        URL url = findResource(path);
        if (url == null) {
            return super.findClass(name);
        }
        byte[] bytes;
        try {
            bytes = Utils.readAllBytes(url.openStream());
            Map<String, byte[]> outputs = ClassDowngrader.currentVersionDowngrader.downgrade(internalName, bytes, new Function<String, byte[]>() {
                @Override
                public byte[] apply(String s) {
                    try {
                        URL url = findResource(s + ".class");
                        if (url == null) return null;
                        return Utils.readAllBytes(url.openStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            if (outputs == null || outputs.isEmpty()) {
                // doesn't need downgrading
                return defineClass(name, bytes, 0, bytes.length);
            }
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                if (entry.getKey().equals(internalName)) continue; // skip the main class (load later and returned)
                String extraName = entry.getKey().replace('/', '.');
                byte[] extraBytes = entry.getValue();
                try {
                    defineClass(extraName, extraBytes, 0, extraBytes.length);
                } catch (ClassFormatError e) {
                    ClassDowngrader.currentVersionDowngrader.writeBytesToDebug(extraName, bytes);
                    throw e;
                }
            }
            try {
                bytes = outputs.get(internalName);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (ClassFormatError e) {
                ClassDowngrader.currentVersionDowngrader.writeBytesToDebug(name, bytes);
                throw e;
            }
        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        }
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
