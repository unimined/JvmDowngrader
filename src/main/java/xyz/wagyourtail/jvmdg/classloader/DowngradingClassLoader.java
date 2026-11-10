package xyz.wagyourtail.jvmdg.classloader;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
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

    /**
     * The api jar is a delegate of this loader, but it may ALSO sit on the
     * app classpath (e.g. tests, cli usage). Plain parent-first delegation
     * would then load provider/stub classes raw — including versions this
     * JVM cannot run — and their stub-class literals would never be
     * transformed (UnsupportedClassVersionError on old JVMs). These packages
     * are loaded child-first so they are defined by this loader and every
     * class literal inside them routes through {@link #transformClass}.
     *
     * <p>Shared packages (util, exc, version, the stub annotations under
     * {@code stub.} itself) deliberately stay parent-first: they must keep
     * class identity with the main jar for things like
     * {@code getAnnotation(Stub.class)}.
     */
    private static boolean isApiClass(String name) {
        if (!name.startsWith("xyz.wagyourtail.jvmdg.")) {
            return false;
        }
        String rest = name.substring("xyz.wagyourtail.jvmdg.".length());
        if (rest.startsWith("providers.") || rest.startsWith("stub.all.")) {
            return true;
        }
        // versioned packages: j8., j9., j10., ...
        if (rest.length() > 2 && rest.charAt(0) == 'j' && Character.isDigit(rest.charAt(1))) {
            int i = 1;
            while (i < rest.length() && Character.isDigit(rest.charAt(i))) {
                i++;
            }
            return i < rest.length() && rest.charAt(i) == '.';
        }
        return false;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null && isApiClass(name)) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
            if (c == null) {
                c = super.loadClass(name, resolve);
            } else if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    @Override
    protected Class<?> transformClass(String name, byte[] classBytes) throws ClassNotFoundException {
        if (classBytes.length >= 8 && (((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF)) <= Utils.getCurrentClassVersion()) {
            // already loadable on this JVM — same result as the downgrader's
            // version<=target early-out, but skips the provider chain entirely.
            // Provider classes land here, which also avoids recursing into
            // provider init while collectProviders() is still running.
            return defineClass(name, classBytes, 0, classBytes.length);
        }
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
    protected int maxClassVersionSupported() {
        if (!holder.flags.downgradeFromMultiReleases) {
            return Utils.getCurrentClassVersion();
        } else {
            return holder.maxVersion();
        }
    }

    @Override
    protected List<String> multiVersionPrefixes() {
        if (holder.maxVersion() == -1) {
            return Collections.singletonList("");
        }
        return super.multiVersionPrefixes();
    }

    @Override
    public void close() throws IOException {
        if (holder != currentVersionDowngrader) {
            currentVersionDowngrader.close();
        }
        super.close();
    }

}
