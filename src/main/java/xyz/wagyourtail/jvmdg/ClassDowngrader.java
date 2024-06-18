package xyz.wagyourtail.jvmdg;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.classloader.DowngradingClassLoader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.*;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class ClassDowngrader implements Closeable {
    /**
     * because parent is null, this is (essentially) a wrapper around the bootstrap classloader
     */
    public static final ClassLoader bootstrapClassLoader = new URLClassLoader(new URL[0], null);
    private static ClassDowngrader currentVersionDowngrader = null;
    public final Flags flags;
    public final int target;
    private final Map<Integer, VersionProvider> downgraders;
    private final DowngradingClassLoader classLoader;

    @ApiStatus.Internal
    public final Logger logger;

    protected ClassDowngrader(@NotNull Flags flags) {
        this.flags = flags;
        this.target = flags.classVersion;
        logger = new Logger(ClassDowngrader.class, flags.printDebug ? Logger.Level.DEBUG : flags.quiet ? Logger.Level.FATAL : Logger.Level.INFO, System.out);
        try {
            classLoader = new DowngradingClassLoader(this, ClassDowngrader.class.getClassLoader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        downgraders = collectProviders();
    }

    @NotNull
    @Contract("_ -> new")
    public static ClassDowngrader downgradeTo(int version) {
        Flags flags = new Flags();
        flags.classVersion = version;
        return new ClassDowngrader(flags);
    }

    @NotNull
    @Contract("_ -> new")
    public static ClassDowngrader downgradeTo(@NotNull Flags flags) {
        return new ClassDowngrader(flags.copy());
    }

    @NotNull
    public static ClassDowngrader getCurrentVersionDowngrader() {
        if (currentVersionDowngrader == null) {
            Flags flags = new Flags();
            flags.classVersion = Utils.getCurrentClassVersion();
            currentVersionDowngrader = new ClassDowngrader(flags);
        }
        return currentVersionDowngrader;
    }

    @NotNull
    @Contract("_ -> new")
    public static ClassDowngrader getCurrentVersionDowngrader(Flags flags) {
        flags = flags.copy();
        flags.classVersion = Utils.getCurrentClassVersion();
        return new ClassDowngrader(flags);
    }

    public DowngradingClassLoader getClassLoader() {
        return classLoader;
    }

    public synchronized Map<Integer, VersionProvider> collectProviders() {
        Map<Integer, VersionProvider> downgraders = new HashMap<>();
        try {
            for (VersionProvider provider : ServiceLoader.load(VersionProvider.class, classLoader)) {
                downgraders.put(provider.inputVersion, provider);
            }
        } catch (Throwable t) {
            try {
                Class.forName("xyz.wagyourtail.jvmdg.providers.Java8Downgrader", false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load downgraders", e);
            }
            t.printStackTrace(System.err);
            throw t;
        }
        return downgraders;
    }

    public Set<MemberNameAndDesc> getMembers(int version, Type type, Set<String> warnings) throws IOException {
        for (int vers = version; vers > target; vers--) {
            VersionProvider downgrader = downgraders.get(vers);
            if (downgrader == null) {
                throw new RuntimeException("Unsupported class version: " + vers + " supported: " + downgraders.keySet());
            }
            downgrader.ensureInit(this);
            Type stubbed = downgrader.stubClass(type, warnings);
            if (!stubbed.equals(type)) {
                try (InputStream stream = classLoader.getResourceAsStream(stubbed.getInternalName() + ".class")) {
                    if (stream == null) throw new IOException("Failed to find stubbed class: " + stubbed);
                    ClassReader reader = new ClassReader(stream);
                    ClassNode node = new ClassNode();
                    reader.accept(node, ClassReader.SKIP_CODE);
                    Set<MemberNameAndDesc> members = new HashSet<>();
                    for (MethodNode o : node.methods) {
                        if ((o.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                        members.add(new MemberNameAndDesc(o.name, stubClass(node.version, Type.getMethodType(o.desc), warnings)));
                    }
                    return members;
                }
            }
        }
        try (InputStream stream = classLoader.getResourceAsStream(type.getInternalName() + ".class")) {
            if (bootstrapClassLoader.getResource(type.getInternalName() + ".class") != null) {
                // assume all java api classes are... "empty". this makes parentOnly stubs work properly.
                return new HashSet<>();
            }
            if (type.getInternalName().startsWith("xyz/wagyourtail/jvmdg")) {
                // also all jvmdg classes are "empty"
                return new HashSet<>();
            }
            if (stream == null) return null;
            ClassReader reader = new ClassReader(stream);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_CODE);
            Set<MemberNameAndDesc> members = new HashSet<>();
            for (MethodNode o : node.methods) {
                if ((o.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                members.add(new MemberNameAndDesc(o.name, stubClass(node.version, Type.getMethodType(o.desc), warnings)));
            }
            return members;
        }
    }

    public List<Pair<Type, Boolean>> getSupertypes(int version, Type type, Set<String> warnings) throws IOException {
        for (int vers = version; vers > target; vers--) {
            VersionProvider downgrader = downgraders.get(vers);
            if (downgrader == null) {
                throw new RuntimeException("Unsupported class version: " + vers + " supported: " + downgraders.keySet());
            }
            downgrader.ensureInit(this);
            Type stubbed = downgrader.stubClass(type, warnings);
            if (!stubbed.equals(type)) {
                try (InputStream stream = classLoader.getResourceAsStream(stubbed.getInternalName() + ".class")) {
                    if (stream == null) throw new IOException("Failed to find stubbed class: " + stubbed);
                    ClassReader reader = new ClassReader(stream);
                    List<Pair<Type, Boolean>> types = new ArrayList<>();
                    types.add(new Pair<>(Type.getObjectType(reader.getSuperName()), Boolean.FALSE));
                    for (String anInterface : reader.getInterfaces()) {
                        types.add(new Pair<>(Type.getObjectType(anInterface), Boolean.TRUE));
                    }
                    return types;
                }
            }
        }
        try (InputStream stream = classLoader.getResourceAsStream(type.getInternalName() + ".class")) {
            if (stream == null) return null;
            ClassReader reader = new ClassReader(stream);
            List<Pair<Type, Boolean>> types = new ArrayList<>();
            types.add(new Pair<>(Type.getObjectType(reader.getSuperName()), Boolean.FALSE));
            for (String anInterface : reader.getInterfaces()) {
                types.add(new Pair<>(Type.getObjectType(anInterface), Boolean.TRUE));
            }
            return types;
        }
    }

    public Boolean isInterface(int version, Type type, Set<String> warnings) throws IOException {
        for (int vers = version; vers > target; vers--) {
            VersionProvider downgrader = downgraders.get(vers);
            if (downgrader == null) {
                throw new RuntimeException("Unsupported class version: " + vers + " supported: " + downgraders.keySet());
            }
            downgrader.ensureInit(this);
            Type stubbed = downgrader.stubClass(type, warnings);
            if (!stubbed.equals(type)) {
                try (InputStream stream = classLoader.getResourceAsStream(stubbed.getInternalName() + ".class")) {
                    if (stream == null) throw new IOException("Failed to find stubbed class: " + stubbed);
                    ClassReader reader = new ClassReader(stream);
                    return (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0;
                }
            }
        }
        try (InputStream stream = classLoader.getResourceAsStream(type.getInternalName() + ".class")) {
            if (stream == null) return null;
            ClassReader reader = new ClassReader(stream);
            return (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0;
        }
    }

    public Type stubClass(int version, Type type, Set<String> warnings) {
        for (int vers = version; vers > target; vers--) {
            VersionProvider downgrader = downgraders.get(vers);
            if (downgrader == null) {
                throw new RuntimeException("Unsupported class version: " + vers + " supported: " + downgraders.keySet());
            }
            downgrader.ensureInit(this);
            Type stubbed = downgrader.stubClass(type, warnings);
            if (!stubbed.equals(type)) {
                return stubbed;
            }
        }
        return type;
    }

    protected Set<ClassNode> downgrade(ClassNode clazz, boolean enableRuntime, Function<String, ClassNode> getReadOnly) throws IOException {
        Set<ClassNode> classes = new HashSet<>();
        classes.add(clazz);
        int version = clazz.version;
        while (version > target) {
            VersionProvider downgrader = downgraders.get(version);
            if (downgrader == null) {
                throw new RuntimeException("Unsupported class version: " + version + " supported: " + downgraders.keySet());
            }
            Set<ClassNode> newClasses = new HashSet<>();
            for (ClassNode c : classes) {
                ClassNode downgraded = downgrader.downgrade(this, c, newClasses, enableRuntime, getReadOnly);
                if (downgraded != null) {
                    newClasses.add(downgraded);
                }
            }
            classes = newClasses;
            version = downgrader.outputVersion;
        }
        return classes;
    }

    public List<VersionProvider> versionProviders(int inputVersion) {
        List<VersionProvider> providers = new ArrayList<>();
        int version = inputVersion;
        while (version > target) {
            VersionProvider downgrader = downgraders.get(version);
            if (downgrader == null) {
                throw new RuntimeException("Unsupported class version: " + version + " supported: " + downgraders.keySet());
            }
            providers.add(downgrader);
            version = downgrader.outputVersion;
        }
        return providers;
    }

    public Map<String, byte[]> downgrade(/* in out */ AtomicReference<String> name, @NotNull byte[] bytes, boolean enableRuntime, final Function<String, byte[]> getExtraRead) throws IllegalClassFormatException {
        // check magic
        if (bytes[0] != (byte) 0xCA || bytes[1] != (byte) 0xFE || bytes[2] != (byte) 0xBA ||
                bytes[3] != (byte) 0xBE) {
            throw new IllegalClassFormatException(name.get());
        }
        // ignore minor version
        // get major version
        int version = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
        if (version <= target) {
            // already at or below the target version
            return null;
        }
        // transform
        ClassNode node = ASMUtils.bytesToClassNode(bytes);
        if (name.get() == null) {
            name.set(node.name);
        } else if (!name.get().equals(node.name)) {
            throw new RuntimeException("Class name mismatch: " + name.get() + " != " + node.name);
        }
        Map<String, byte[]> outputs = new HashMap<>();
        try {
            if (flags.printDebug) System.out.println("Transforming " + name.get());
            Set<ClassNode> extra = downgrade(node, enableRuntime, new Function<String, ClassNode>() {

                @Override
                public ClassNode apply(String s) {
                    try {
                        byte[] out = getExtraRead.apply(s);
                        if (out == null) {
                            return null;
                        }
                        return ASMUtils.bytesToClassNode(out);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            for (ClassNode c : extra) {
                if (flags.printDebug) {
                    File f = new File(Constants.DEBUG_DIR, c.name + ".javasm");
                    f.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(f)) {
                        TraceClassVisitor tcv = new TraceClassVisitor(null, new Textifier(), new PrintWriter(fos));
                        c.accept(tcv);
                    } catch (IOException ignored) {
                    }
                }
                outputs.put(c.name, classNodeToBytes(c));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to downgrade " + name.get(), e);
        }
        if (logger.is(Logger.Level.DEBUG)) {
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                if (!entry.getKey().equals(name.get())) {
                    logger.debug("Downgraded " + entry.getKey() + " from unknown to " + target);
                } else {
                    logger.debug("Downgraded " + entry.getKey() + " from " + version + " to " + target);
                }
                writeBytesToDebug(entry.getKey(), entry.getValue());
            }
        }
        return outputs;
    }

    public byte[] classNodeToBytes(@NotNull final ClassNode node) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(cw);
        return cw.toByteArray();
    }

    public void writeBytesToDebug(@NotNull String name, byte[] bytes) {
        File f = new File(Constants.DEBUG_DIR, name.replace('.', '/') + ".class");
        f.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(bytes);
        } catch (IOException ignored) {
        }
    }

    public void stub(int versionOpcode, Class<?> clazz) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        downgraders.get(versionOpcode).stub(clazz);
    }

    @VisibleForTesting
    public VersionProvider getVersionProviderFor(int version) {
        // This is not true for java 1
        VersionProvider vp = downgraders.get(version + 44);
        if (vp == null) return null;
        vp.ensureInit(this);
        return vp;
    }

    @Override
    public void close() throws IOException {
        classLoader.close();
    }

}
