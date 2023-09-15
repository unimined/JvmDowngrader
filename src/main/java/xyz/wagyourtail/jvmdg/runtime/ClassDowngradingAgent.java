package xyz.wagyourtail.jvmdg.runtime;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cursed.UnsafeWrapper;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassDowngradingAgent implements ClassFileTransformer {
    public static final MethodHandle defineClass;
    private static final Logger LOGGER = Logger.getLogger("JVMDowngrader/Agent");
    public static final boolean DUMP_CLASSES = Boolean.parseBoolean(System.getProperty("jvmdg.dump", "true"));

    static {
        LOGGER.setLevel(Boolean.parseBoolean(System.getProperty("jvmdg.log", "true")) ? Level.ALL : Level.OFF);
    }

    static {
        Method md;
        try {
            md = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            md.setAccessible(true);
            defineClass = MethodHandles.lookup().unreflect(md);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final int currentVersion;

    static {
        String version = System.getProperty("java.class.version");
        if (version != null) {
            try {
                currentVersion = Integer.parseInt(version.split("\\.")[0]);
            } catch (NumberFormatException e) {
                throw new UnsupportedOperationException("Unable to determine current class version");
            }
        } else {
            throw new UnsupportedOperationException("Unable to determine current class version");
        }
    }

    @Override
    public byte[] transform(final ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
        try {
            if (protectionDomain != null) {
                // remove signers
                UnsafeWrapper.putObject(protectionDomain.getCodeSource(), UnsafeWrapper.objectFieldOffset(CodeSource.class.getDeclaredField("signers")), null);
                UnsafeWrapper.putObject(protectionDomain.getCodeSource(), UnsafeWrapper.objectFieldOffset(CodeSource.class.getDeclaredField("certs")), null);
                UnsafeWrapper.putObject(protectionDomain.getCodeSource(), UnsafeWrapper.objectFieldOffset(CodeSource.class.getDeclaredField("factory")), null);
            }
            // check magic
            if (bytes[0] != (byte) 0xCA || bytes[1] != (byte) 0xFE || bytes[2] != (byte) 0xBA ||
                    bytes[3] != (byte) 0xBE) {
                throw new IllegalClassFormatException(className);
            }
            // ignore minor version
            // get major version
            int version = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
            if (version <= currentVersion) {
                // already at or below the target version
//            LOGGER.info("Ignoring " + className + " as it is already at or below the target version");
                return null;
            }
            LOGGER.info("Transforming " + className + " from " + version + " to " + currentVersion);
//        if (loader instanceof DowngradingClassLoader) return bytes; // already handled
            String internalName = className.replace('.', '/');
            Map<String, byte[]> outputs = ClassDowngrader.currentVersionDowngrader.downgrade(new AtomicReference<>(internalName), bytes, new Function<String, byte[]>() {
                @Override
                public byte[] apply(String s) {
                    try {
                        URL url = loader.getResource(s + ".class");
                        if (url == null) return null;
                        return Utils.readAllBytes(url.openStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            LOGGER.info("transform size: " + (outputs == null ? null : outputs.size()));
            if (outputs == null || outputs.isEmpty()) return bytes;
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                LOGGER.info("Loading " + entry.getKey() + " into " + loader);
                if (DUMP_CLASSES) {
                    Utils.dumpClass(entry.getKey(), entry.getValue());
                }
                if (entry.getKey().equals(className)) {
                    bytes = entry.getValue();
                    continue;
                }
                try {
                    defineClass.bindTo(loader).invoke(entry.getKey().replace('/', '.'), entry.getValue(), 0, entry.getValue().length);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
            return bytes;
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Failed to transform " + className, t);
            System.exit(1);
            return null;
        }
    }

}
