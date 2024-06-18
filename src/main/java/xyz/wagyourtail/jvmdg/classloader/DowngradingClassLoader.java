package xyz.wagyourtail.jvmdg.classloader;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.classloader.providers.ClassLoaderResourceProvider;
import xyz.wagyourtail.jvmdg.classloader.providers.JarFileResourceProvider;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;

public class DowngradingClassLoader extends ClassLoader implements Closeable {
    private final ClassDowngrader holder;
    private final ClassDowngrader currentVersionDowngrader;
    private final List<ResourceProvider> delegates = new ArrayList<>();

    private final Logger logger;

    public DowngradingClassLoader(ClassDowngrader downgrader) throws IOException {
        super();
        File apiJar = downgrader.flags.findJavaApi();
        if (apiJar != null) {
            delegates.add(new JarFileResourceProvider(new JarFile(apiJar)));
        }
        this.holder = downgrader;
        if (downgrader.target != Utils.getCurrentClassVersion()) {
            this.currentVersionDowngrader = ClassDowngrader.getCurrentVersionDowngrader(downgrader.flags);
        } else {
            this.currentVersionDowngrader = downgrader;
        }
        logger = holder.logger.subLogger(DowngradingClassLoader.class);
    }

    public DowngradingClassLoader(ClassDowngrader downgrader, ClassLoader parent) throws IOException {
        super(parent);
        File apiJar = downgrader.flags.findJavaApi();
        if (apiJar != null) {
            delegates.add(new JarFileResourceProvider(new JarFile(apiJar)));
        }
        this.holder = downgrader;
        if (downgrader.target != Utils.getCurrentClassVersion()) {
            this.currentVersionDowngrader = ClassDowngrader.getCurrentVersionDowngrader(downgrader.flags);
        } else {
            this.currentVersionDowngrader = downgrader;
        }
        logger = holder.logger.subLogger(DowngradingClassLoader.class);
    }

    public DowngradingClassLoader(ClassDowngrader downgrader, List<ResourceProvider> providers, ClassLoader parent) throws IOException {
        this(downgrader, parent);
        delegates.addAll(providers);
    }

    public DowngradingClassLoader(ClassDowngrader downgrader, List<ResourceProvider> providers) throws IOException {
        this(downgrader);
        delegates.addAll(providers);
    }

    public void addDelegate(ClassLoader loader) {
        delegates.add(new ClassLoaderResourceProvider(loader));
    }

    public void addDelegate(JarFile jarFile) {
        delegates.add(new JarFileResourceProvider(jarFile));
    }

    public void addDelegate(URL[] urls) {
        delegates.add(new ClassLoaderResourceProvider(new URLClassLoader(urls)));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String internalName = name.replace('.', '/');
        String path = internalName + ".class";
        URL url = findResource(path);
        if (url == null) {
            return super.findClass(name);
        }
        byte[] bytes = null;
        try {
            bytes = Utils.readAllBytes(url.openStream());
            Map<String, byte[]> outputs = currentVersionDowngrader.downgrade(new AtomicReference<>(internalName), bytes, true, new Function<String, byte[]>() {
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
            if (outputs == null) {
                // doesn't need downgrading
                return defineClass(name, bytes, 0, bytes.length);
            }
            Class<?> returnValue;
            try {
                bytes = outputs.get(internalName);
                if (bytes == null) {
                    throw new ClassNotFoundException("removed by downgrader: " + name);
                }
                returnValue = defineClass(name, bytes, 0, bytes.length);
            } catch (ClassFormatError e) {
                currentVersionDowngrader.writeBytesToDebug(name, bytes);
                logger.fatal("Failed to load class " + name + " with downgraded bytes, writing to debug folder.", e);
                throw new ClassNotFoundException(name, e);
            }
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                if (entry.getKey().equals(internalName)) continue; // skip the main class (load later and returned)
                String extraName = entry.getKey().replace('/', '.');
                byte[] extraBytes = entry.getValue();
                try {
                    defineClass(extraName, extraBytes, 0, extraBytes.length);
                } catch (ClassFormatError | ClassCircularityError e) {
                    logger.fatal("Failed to load class " + extraName + " with downgraded bytes, writing to debug folder.", e);
                    currentVersionDowngrader.writeBytesToDebug(extraName, bytes);
                    throw e;
                }
            }
            return returnValue;
        } catch (ClassFormatError e) {
            currentVersionDowngrader.writeBytesToDebug(name, bytes);
            logger.fatal("Failed to load class " + name + " with original bytes, writing to debug folder.", e);
            throw new ClassNotFoundException(name, e);
        } catch (Throwable e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    @Override
    protected URL findResource(String name) {
        return findResources(name).nextElement();
    }

    @Override
    protected Enumeration<URL> findResources(final String name) {
        return new FlatEnumeration<>(Collections.enumeration(delegates), new Function<ResourceProvider, Enumeration<URL>>() {
            @Override
            public Enumeration<URL> apply(ResourceProvider resourceProvider) {
                try {
                    return resourceProvider.getResources(name);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void close() throws IOException {
        if (holder != currentVersionDowngrader) {
            currentVersionDowngrader.close();
        }
        for (ResourceProvider delegate : delegates) {
            delegate.close();
        }
    }
}
