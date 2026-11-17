package xyz.wagyourtail.jvmdg.runtime;

import org.objectweb.asm.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ClassDowngradingAgent implements ClassFileTransformer {
    public static final MethodHandle defineClass;
    public static final boolean DUMP_CLASSES = Boolean.parseBoolean(System.getProperty("jvmdg.dump", "false"));
    private static final Logger LOGGER = Bootstrap.LOGGER.subLogger("Agent");
    private static final int currentVersion;
    private static volatile List<Integer> multiVersions;

    static {
        Method md;
        try {
            md = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            defineClass = Utils.getImplLookup().unreflect(md);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * computed lazily: {@link ClassDowngrader#maxVersion()} is -1 until the
     * version providers initialize, so snapshotting at class init can leave
     * this permanently empty.
     */
    protected List<Integer> multiVersions() {
        List<Integer> versions = multiVersions;
        int max = Bootstrap.currentVersionDowngrader.maxVersion();
        if (versions == null || (!versions.isEmpty() && max > versions.get(0))) {
            if (max == -1) {
                return Collections.emptyList(); // providers not initialized yet; retry next call
            }
            versions = new ArrayList<>();
            for (int i = max; i > Bootstrap.currentVersionDowngrader.target; i--) {
                versions.add(i);
            }
            multiVersions = versions;
        }
        return versions;
    }

    static {
        currentVersion = Utils.getCurrentClassVersion();
    }

    public byte[] retransformCodeSource(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                // the aim is to null the certs/signers parameter of the
                // CodeSource(URL, ...) ctors before the body reads it. Guarded:
                // only when a second parameter actually exists, is a reference
                // type, and lands on a computable local slot — an unguarded
                // ASTORE 2 corrupts any ctor with fewer/differently-sized args
                if (name.equals("<init>")) {
                    Type[] args = Type.getArgumentTypes(descriptor);
                    if (args.length >= 2 &&
                        (args[1].getSort() == Type.OBJECT || args[1].getSort() == Type.ARRAY)) {
                        final int slot = 1 + args[0].getSize(); // local slot of the 2nd arg
                        return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                visitInsn(Opcodes.ACONST_NULL);
                                visitVarInsn(Opcodes.ASTORE, slot);
                            }
                        };
                    }
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    @Override
    public byte[] transform(final ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
        try {
            if (className != null && className.equals("java/security/CodeSource")) {
                return retransformCodeSource(bytes);
            }

            if (loader != null && className != null && Bootstrap.flags.downgradeFromMultiReleases) {
                for (Integer version : multiVersions()) {
                    try (InputStream stream = openResource(loader, "META-INF/versions/" + Utils.classVersionToMajorVersion(version) + "/" + className + ".class")) {
                        if (stream != null) {
                            bytes = Utils.readAllBytes(stream);
                            break;
                        }
                    }
                }
            }

            // check magic
            if (bytes.length < 8 || bytes[0] != (byte) 0xCA || bytes[1] != (byte) 0xFE || bytes[2] != (byte) 0xBA ||
                bytes[3] != (byte) 0xBE) {
                throw new IllegalClassFormatException(className);
            }
            // ignore minor version
            // get major version
            int version = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
            if (version <= currentVersion) {
                // already at or below the target version
                LOGGER.trace("Ignoring " + className + " as it is already at or below the target version");
                return null;
            }
            LOGGER.trace("Transforming " + className + " from " + version + " to " + currentVersion);
//        if (loader instanceof DowngradingClassLoader) return bytes; // already handled
            final AtomicReference<String> resolvedName = new AtomicReference<>(className);
            // bootstrap loads (loader == null) have no ClassLoader to read
            // resources through; use the bootstrap-delegating loader instead
            final ClassLoader resourceLoader = loader == null ? ClassDowngrader.bootstrapClassLoader : loader;
            Map<String, byte[]> outputs = Bootstrap.currentVersionDowngrader.downgrade(resolvedName, bytes, true, s -> {
                try {
                    if (loader != null && Bootstrap.flags.downgradeFromMultiReleases) {
                        for (Integer multiVersion : multiVersions()) {
                            try (InputStream stream = openResource(resourceLoader, "META-INF/versions/" + Utils.classVersionToMajorVersion(multiVersion) + "/" + s + ".class")) {
                                if (stream != null) {
                                    return Utils.readAllBytes(stream);
                                }
                            }
                        }
                    }
                    try (InputStream stream = openResource(resourceLoader, s + ".class")) {
                        if (stream != null) {
                            return Utils.readAllBytes(stream);
                        }
                    }
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            LOGGER.trace("transform size for " + resolvedName.get() + ": " + (outputs == null ? null : outputs.size()));
            if (outputs == null) return bytes;
            // downgrade() resolves a null name from the class bytes, so this is non-null here
            String name = resolvedName.get();
            bytes = null;
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                LOGGER.trace("Loading " + entry.getKey() + " into " + loader);
                if (DUMP_CLASSES) {
                    Bootstrap.currentVersionDowngrader.writeBytesToDebug(entry.getKey(), entry.getValue());
                }
                if (entry.getKey().equals(name)) {
                    bytes = entry.getValue();
                    continue;
                }
                if (entry.getKey().startsWith("META-INF/versions")) continue; // skip the versioned classes
                if (loader == null) continue; // extras for bootstrap-loaded classes resolve via the classpath
                String extraName = entry.getKey().replace('/', '.');
                try {
                    defineClass.bindTo(loader).invoke(extraName, entry.getValue(), 0, entry.getValue().length);
                } catch (LinkageError e) {
                    // a raw defineClass can race a loadClass for the same name
                    // on another thread; the racing definition is equivalent,
                    // so keep theirs rather than kill the transform
                    LOGGER.warn("Skipping already-defined class " + extraName + " (" + e + ")");
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
            return bytes;
        } catch (IllegalClassFormatException e) {
            throw e;
        } catch (Throwable t) {
            // a transformer must never kill the host jvm; report the failure and
            // let the vm fall back to the original class bytes
            LOGGER.error("Failed to transform " + className, t);
            IllegalClassFormatException e = new IllegalClassFormatException(className);
            e.initCause(t);
            throw e;
        }
    }

    private static InputStream openResource(ClassLoader loader, String path) throws IOException {
        return Utils.openResource(loader, path);
    }

}
