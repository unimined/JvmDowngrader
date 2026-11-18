package xyz.wagyourtail.jvmdg;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.asm.index.ClasspathIndex;
import xyz.wagyourtail.jvmdg.asm.index.NamespaceView;
import xyz.wagyourtail.jvmdg.classloader.DowngradingClassLoader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.pipeline.TransformPipeline;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.DowngradeRegistrar;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.*;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ClassDowngrader implements Closeable {
    /**
     * because parent is null, this is (essentially) a wrapper around the bootstrap classloader
     */
    public static final ClassLoader bootstrapClassLoader = new URLClassLoader(new URL[0], null);
    private static ClassDowngrader currentVersionDowngrader = null;
    public final Flags flags;
    public final int target;
    @ApiStatus.Internal
    public final Logger logger;
    private final Map<Integer, VersionProvider> downgraders;
    private final DowngradingClassLoader classLoader;
    public final ClasspathIndex classpathIndex;
    protected volatile int maxVersion = -1;

    protected ClassDowngrader(@NotNull Flags flags) {
        this.flags = flags;
        this.target = flags.classVersion;
        logger = new Logger(ClassDowngrader.class, flags.getLogLevel(), flags.logAnsiColors, System.out);
        try {
            classLoader = new DowngradingClassLoader(this, ClassDowngrader.class.getClassLoader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        classpathIndex = new ClasspathIndex(classLoader);
        downgraders = collectProviders();
    }

    public int maxVersion() {
        if (downgraders == null) {
            return -1;
        }
        if (maxVersion == -1) {
            synchronized (this) {
                if (maxVersion == -1) {
                    int max = target;
                    Set<Integer> ints = downgraders.keySet();
                    for (Integer i : ints) {
                        max = Math.max(max, i);
                    }
                    maxVersion = max;
                }
            }
        }
        return maxVersion;
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
    public static synchronized ClassDowngrader getCurrentVersionDowngrader() {
        // synchronized: unsynchronized lazy init could construct the shared
        // downgrader twice (leaking a classloader/providers) or publish a
        // partially-constructed instance to racing callers
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

    /**
     * Every {@link VersionProvider} discovered at construction, keyed by
     * {@code inputVersion} — includes providers below {@link #target} that
     * no run's version chain will ever initialize (needed when a transform
     * wants provider data outside the chain, e.g. the noIndy desugar
     * union). Read-only view; these are the same instances the chain
     * initializes.
     */
    public Map<Integer, VersionProvider> providers() {
        return java.util.Collections.unmodifiableMap(downgraders);
    }

    /**
     * Registers an already-constructed provider into the boundary map —
     * used at index-build time ({@code StubIndexWriter}) where providers
     * are constructed programmatically from the version table rather than
     * ServiceLoader-discovered off the api jar.
     */
    public void addProvider(VersionProvider provider) {
        downgraders.put(provider.inputVersion, provider);
    }

    public synchronized Map<Integer, VersionProvider> collectProviders() {
        Map<Integer, VersionProvider> downgraders = new HashMap<>();
        try {
            for (VersionProvider provider : ServiceLoader.load(VersionProvider.class, classLoader)) {
                if (downgraders.containsKey(provider.inputVersion)) {
                    VersionProvider prev = downgraders.get(provider.inputVersion);
                    if (prev.priority < provider.priority) {
                        downgraders.put(provider.inputVersion, provider);
                    } else if (prev.priority == provider.priority) {
                        logger.warn(
                            "Duplicate version providers with same priority for " + provider.inputVersion
                                + " \"" + provider.getClass().getName() + "\" and \"" + prev.getClass().getName() + "\""
                        );
                    }
                } else {
                    downgraders.put(provider.inputVersion, provider);
                }
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

    /**
     * Query surface over the classpath as seen from {@code version} — every
     * query remaps the queried type through the provider chain (first-hit
     * wins) before resolving against the index.
     */
    public NamespaceView namespace(int version, Set<String> warnings) {
        return new NamespaceView(this, version, warnings);
    }

    public Set<MemberNameAndDesc> getMembers(int version, Type type, Set<String> warnings) throws IOException {
        return namespace(version, warnings).membersAt(type);
    }

    public List<Pair<Type, Boolean>> getSupertypes(int version, Type type, Set<String> warnings) throws IOException {
        return namespace(version, warnings).supertypesAt(type);
    }

    public Boolean isInterface(int version, Type type, Set<String> warnings) throws IOException {
        return namespace(version, warnings).isInterfaceAt(type);
    }

    public Type stubClass(int version, Type type, Set<String> warnings) {
        for (int vers = version; vers > target; ) {
            VersionProvider downgrader = downgraders.get(vers);
            if (downgrader == null) {
                throw new RuntimeException("Unsupported class version: " + vers + " supported: " + downgraders.keySet());
            }
            vers = downgrader.outputVersion;
            downgrader.ensureInit(this);
            Type stubbed = downgrader.stubClass(type, warnings);
            if (!stubbed.equals(type)) {
                return stubbed;
            }
        }
        return type;
    }

    private static final Comparator<ClassNode> CLASS_NODE_NAME_ORDER =
        Comparator.comparing(c -> c.name);

    /**
     * multi-release captures share the class name but differ in {@code version}
     * — name alone would dedup them away (dropping the final output when a
     * capture lands first).
     */
    private static final Comparator<ClassNode> CLASS_NODE_NAME_VERSION_ORDER =
        Comparator.comparing((ClassNode c) -> c.name).thenComparingInt(c -> c.version);

    protected Set<ClassNode> downgrade(ClassNode clazz, boolean enableRuntime, Function<String, ClassNode> getReadOnly) throws IOException {
        // the registrar emits one stage per provider boundary; sequential
        // pipeline execution reproduces the legacy per-provider chain —
        // sorted result so output order (and therefore define order in
        // loaders and jar entry order) is deterministic
        DowngradeRegistrar registrar = new DowngradeRegistrar(this, enableRuntime, getReadOnly);
        TransformPipeline pipeline = new TransformPipeline().addStages(registrar.stages(clazz.version));
        TransformContext ctx = new TransformContext(classpathIndex, flags, logger);
        registrar.install(ctx);
        List<ClassNode> out = pipeline.run(Collections.singletonList(clazz), ctx);
        Set<ClassNode> result = new TreeSet<>(CLASS_NODE_NAME_VERSION_ORDER);
        for (Map.Entry<Object, List<ClassNode>> e : pipeline.variants().entrySet()) {
            result.addAll(e.getValue());
        }
        result.addAll(out);
        return result;
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
        if (bytes.length < 8 || bytes[0] != (byte) 0xCA || bytes[1] != (byte) 0xFE ||
            bytes[2] != (byte) 0xBA || bytes[3] != (byte) 0xBE) {
            throw new IllegalClassFormatException(name.get());
        }
        // ignore minor version
        // get major version
        int version = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
        if (!flags.allowPreview && (((bytes[4] & 0xFF) << 8) | (bytes[5] & 0xFF)) == 0xFFFF) {
            // javac --enable-preview sets minor_version = 0xFFFF — refuse
            // silently processing such inputs unless the run opted in
            throw new IllegalClassFormatException(name.get() +
                " (preview-compiled, rerun with --allowPreview)");
        }
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
        Map<String, byte[]> outputs = new TreeMap<>();
        try {
            logger.trace("Transforming " + name.get());
            // per-call memoization: within one downgrade the resolvers query
            // the same input-jar/loader classes repeatedly (hierarchy walks).
            // scoped to this call — the underlying source may return different
            // bytes for the same name in later calls (e.g. a different loader).
            final Map<String, Object> originalCache = new HashMap<>();
            final Object miss = new Object();
            Set<ClassNode> extra = downgrade(node, enableRuntime, s -> {
                Object cached = originalCache.get(s);
                if (cached == null) {
                    try {
                        byte[] out = getExtraRead.apply(s);
                        cached = out == null ? miss : ASMUtils.bytesToClassNode(out);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    originalCache.put(s, cached);
                }
                return cached == miss ? null : (ClassNode) cached;
            });
            boolean hasVersions = false;
            for (ClassNode c : extra) {
                byte[] cBytes = ASMUtils.classNodeToBytes(c, ClassWriter.COMPUTE_MAXS);
                if (c.version > target) {
                    // write to multi-release location
                    outputs.put("META-INF/versions/" + Utils.classVersionToMajorVersion(c.version) + "/" + c.name, cBytes);
                    hasVersions = true;
                } else {
                    outputs.put(c.name, cBytes);
                }
            }
            if (hasVersions) {
                // filter outputs that are effectively the same
                Map<String, Map<Integer, byte[]>> byVersion = new TreeMap<>();
                for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith("META-INF/versions/")) {
                        int major = Integer.parseInt(key.substring(18, key.indexOf('/', 18)));
                        String cName = key.substring(key.indexOf('/', 18) + 1);
                        if (!byVersion.containsKey(cName)) {
                            byVersion.put(cName, new TreeMap<Integer, byte[]>());
                        }
                        byVersion.get(cName).put(major, entry.getValue());
                    } else {
                        if (!byVersion.containsKey(key)) {
                            byVersion.put(key, new TreeMap<Integer, byte[]>());
                        }
                        byVersion.get(key).put(0, entry.getValue());
                    }
                }
                outputs.clear();
                Map<String, byte[]> current = new TreeMap<>();
                for (Map.Entry<String, Map<Integer, byte[]>> entry : byVersion.entrySet()) {
                    String key = entry.getKey();
                    Map<Integer, byte[]> versions = entry.getValue();
                    if (!versions.containsKey(0)) {
                        throw new IllegalStateException("Found multi-release class " + key + " without main version!");
                    }
                    if (versions.size() == 1) {
                        outputs.put(key, versions.get(0));
                    }
                    // order will be in numeric increasing order due to how TreeMap works :)
                    for (Map.Entry<Integer, byte[]> vs : versions.entrySet()) {
                        if (vs.getKey() == 0) {
                            current.put(key, vs.getValue());
                            outputs.put(key, vs.getValue());
                        } else {
                            byte[] currentVal = current.get(key);
                            byte[] vsVal = vs.getValue();
                            // equal after version info
                            if (!Utils.equals(currentVal, 8, currentVal.length, vsVal, 8, vsVal.length)) {
                                current.put(key, vsVal);
                                outputs.put("META-INF/versions/" + vs.getKey() + "/" + key, vsVal);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to downgrade " + name.get(), e);
        }
        if (logger.is(Logger.Level.DEBUG) || flags.debugDumpClasses) {
            logger.debug("Classes from " + name.get());
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                if (!entry.getKey().equals(name.get())) {
                    logger.debug("Downgraded " + entry.getKey() + " from unknown to " + target);
                } else {
                    logger.debug("Downgraded " + entry.getKey() + " from " + version + " to " + target);
                }
                if (flags.debugDumpClasses) {
                    writeBytesToDebug(entry.getKey(), entry.getValue());
                }
            }
        }
        return outputs;
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
        VersionProvider vp = downgraders.get(Utils.majorVersionToClassVersion(version));
        if (vp == null) return null;
        vp.ensureInit(this);
        return vp;
    }

    @Override
    public void close() throws IOException {
        classLoader.close();
    }

}
