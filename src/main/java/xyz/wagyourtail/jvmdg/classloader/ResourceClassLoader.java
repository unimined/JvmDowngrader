package xyz.wagyourtail.jvmdg.classloader;

import xyz.wagyourtail.jvmdg.classloader.providers.ClassLoaderResourceProvider;
import xyz.wagyourtail.jvmdg.classloader.providers.JarFileResourceProvider;
import xyz.wagyourtail.jvmdg.collection.FlatMapEnumeration;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;

public class ResourceClassLoader extends ClassLoader implements Closeable {
    private final List<String> multiVersionList = new ArrayList<>();
    private final List<ResourceProvider> delegates = new ArrayList<>();

    public ResourceClassLoader(ClassLoader parent) {
        super(parent);
    }

    public ResourceClassLoader(List<ResourceProvider> resources, ClassLoader parent) {
        super(parent);
        this.delegates.addAll(resources);
    }

    public ResourceClassLoader(Set<URL> urls, ClassLoader parent) throws URISyntaxException, IOException {
        super(parent);
        Set<URL> failed = new HashSet<>();
        for (URL url : urls) {
            try {
                delegates.add(new JarFileResourceProvider(new JarFile(Paths.get(url.toURI()).toFile())));
            } catch (Exception e) {
                failed.add(url);
            }
        }
        // fallback on normal classloader
        if (!failed.isEmpty()) {
            addDelegate(failed.toArray(new URL[0]));
        }
    }

    public void addDelegate(ClassLoader loader) {
        delegates.add(new ClassLoaderResourceProvider(loader));
    }

    public void addDelegate(JarFile jar) {
        delegates.add(new JarFileResourceProvider(jar));
    }

    public void addDelegate(URL[] urls) {
        delegates.add(new ClassLoaderResourceProvider(new URLClassLoader(urls)));
    }

    protected int maxClassVersionSupported() {
        return Utils.getCurrentClassVersion();
    }

    protected List<String> multiVersionPrefixes() {
        if (multiVersionList.isEmpty()) {
            synchronized (multiVersionList) {
                if (multiVersionList.isEmpty()) {
                    for (int i = maxClassVersionSupported(); i >= 51; --i) {
                        if (i == 51) {
                            multiVersionList.add("");
                        } else {
                            multiVersionList.add("META-INF/versions/" + Utils.classVersionToMajorVersion(i) + "/");
                        }
                    }
                }
            }
        }
        return multiVersionList;
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String internalName = name.replace('.', '/');
        String path = internalName + ".class";
        URL resource = findResource(path);
        if (resource == null) {
            return super.findClass(name);
        }
        try (InputStream is = resource.openStream()) {
            return transformClass(name, Utils.readAllBytes(is));
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    protected Class<?> transformClass(String name, byte[] classBytes) throws ClassNotFoundException {
        return defineClass(name, classBytes, 0, classBytes.length);
    }

    @Override
    protected URL findResource(String name) {
        return findResources(name).nextElement();
    }

    @Override
    protected Enumeration<URL> findResources(final String name) {
        return new FlatMapEnumeration<>(Collections.enumeration(name.startsWith("META-INF/versions/") ? Collections.singleton("") : multiVersionPrefixes()), new Function<String, Enumeration<URL>>() {
            @Override
            public Enumeration<URL> apply(final String prefix) {
                return new FlatMapEnumeration<>(Collections.enumeration(delegates), new Function<ResourceProvider, Enumeration<URL>>() {
                    @Override
                    public Enumeration<URL> apply(ResourceProvider resourceProvider) {
                        try {
                            return resourceProvider.getResources(prefix + name);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void close() throws IOException {
        for (ResourceProvider delegate : delegates) {
            delegate.close();
        }
    }

}
