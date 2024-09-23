package xyz.wagyourtail.jvmdg.classloader;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.collection.FlatMapEnumeration;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;

public class DowngradingClassLoader extends ResourceClassLoader implements Closeable {
    private final List<Integer> multiVersionList = new ArrayList<>();

    private final ClassDowngrader holder;
    private final ClassDowngrader currentVersionDowngrader;

    private final Logger logger;

    public DowngradingClassLoader(ClassDowngrader downgrader) throws IOException {
        super(null);
        List<File> apiJar = downgrader.flags.findJavaApi();
        if (apiJar != null) {
            for (File file : apiJar) {
                addDelegate(new JarFile(file));
            }
        }
        this.holder = downgrader;
        if (downgrader.target != Utils.getCurrentClassVersion()) {
            this.currentVersionDowngrader = ClassDowngrader.getCurrentVersionDowngrader(downgrader.flags);
        } else {
            this.currentVersionDowngrader = downgrader;
        }
        logger = holder.logger.subLogger(DowngradingClassLoader.class);
        for (int i = holder.maxVersion(); i >= 52; i--) {
            multiVersionList.add(i);
        }
        multiVersionList.add(-1);
    }

    public DowngradingClassLoader(ClassDowngrader downgrader, ClassLoader parent) throws IOException {
        this(downgrader, Collections.<ResourceProvider>emptyList(), parent);
    }

    public DowngradingClassLoader(ClassDowngrader downgrader, List<ResourceProvider> provider, ClassLoader parent) throws IOException {
        super(provider, parent);
        List<File> apiJar = downgrader.flags.findJavaApi();
        if (apiJar != null) {
            for (File file : apiJar) {
                addDelegate(new JarFile(file));
            }
        }
        this.holder = downgrader;
        if (downgrader.target != Utils.getCurrentClassVersion()) {
            this.currentVersionDowngrader = ClassDowngrader.getCurrentVersionDowngrader(downgrader.flags);
        } else {
            this.currentVersionDowngrader = downgrader;
        }
        logger = holder.logger.subLogger(DowngradingClassLoader.class);
    }

    public DowngradingClassLoader(ClassDowngrader downgrader, List<ResourceProvider> providers) throws IOException {
        this(downgrader, providers, null);
    }

    @Override
    protected Class<?> transformClass(String name, byte[] classBytes) throws ClassNotFoundException {
        String internalName = name.replace('.', '/');
        try {
            Map<String, byte[]> outputs = currentVersionDowngrader.downgrade(new AtomicReference<>(internalName), classBytes, true, new Function<String, byte[]>() {
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
                return defineClass(name, classBytes, 0, classBytes.length);
            }
            Class<?> returnValue;
            try {
                classBytes = outputs.get(internalName);
                if (classBytes == null) {
                    throw new ClassNotFoundException("removed by downgrader: " + internalName);
                }
                returnValue = defineClass(name, classBytes, 0, classBytes.length);
            } catch (ClassFormatError e) {
                currentVersionDowngrader.writeBytesToDebug(name, classBytes);
                logger.fatal("Failed to load class " + name + " with downgraded bytes, writing to debug folder.", e);
                throw new ClassNotFoundException(name, e);
            }
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                if (entry.getKey().equals(internalName)) continue; // skip the main class (load later and returned)
                if (entry.getKey().startsWith("META-INF/versions")) continue; // skip the versioned classes
                String extraName = entry.getKey().replace('/', '.');
                byte[] extraBytes = entry.getValue();
                try {
                    defineClass(extraName, extraBytes, 0, extraBytes.length);
                } catch (ClassFormatError | ClassCircularityError e) {
                    logger.fatal("Failed to load class " + extraName + " with downgraded bytes, writing to debug folder.", e);
                    currentVersionDowngrader.writeBytesToDebug(extraName, extraBytes);
                    throw e;
                }
            }
            return returnValue;
        } catch (IllegalClassFormatException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    @Override
    protected Enumeration<URL> findResources(final String name) {
        return new FlatMapEnumeration<>(Collections.enumeration(multiVersionList), new Function<Integer, Enumeration<URL>>() {
            @Override
            public Enumeration<URL> apply(Integer integer) {
                if (integer != -1) {
                    int major = Utils.classVersionToMajorVersion(integer);
                    return DowngradingClassLoader.super.findResources("META-INF/versions/" + major + "/" + name);
                } else {
                    return DowngradingClassLoader.super.findResources(name);
                }
            }
        });
    }

    @Override
    public void close() throws IOException {
        if (holder != currentVersionDowngrader) {
            currentVersionDowngrader.close();
        }
        super.close();
    }

}
