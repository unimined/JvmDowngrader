package xyz.wagyourtail.jvmdg.runtime;

import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.classloader.DowngradingClassLoader;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Map;

public class ClassDowngradingAgent implements ClassFileTransformer {
    public static final MethodHandle defineClass;

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

    @Override
    public byte[] transform(final ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
        if (loader instanceof DowngradingClassLoader) return bytes; // already handled
        String internalName = className.replace('.', '/');
        Map<String, byte[]> outputs = ClassDowngrader.currentVersionDowngrader.downgrade(internalName, bytes, new Function<String, byte[]>() {
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
        if (outputs == null || outputs.isEmpty()) return bytes;
        for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
            if (entry.getKey().equals(className)) continue;
            try {
                defineClass.bindTo(loader).invoke(entry.getKey().replace('/', '.'), entry.getValue(), 0, entry.getValue().length);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
        return bytes;
    }

}
