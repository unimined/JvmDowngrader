package xyz.wagyourtail.jvmdg;

import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.classloader.DowngradingClassLoader;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.beans.XMLDecoder;
import java.io.*;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ClassDowngrader {
    public static final ClassDowngrader currentVersionDowngrader = new ClassDowngrader(Utils.getCurrentClassVersion());

    // because parent is null, this is (essentially) a wrapper around the bootstrap classloader
    public static final ClassLoader bootstrapClassLoader = new URLClassLoader(new URL[0], null);

    public static final URL javaApi = findJavaApi();
    public static final DowngradingClassLoader classLoader = new DowngradingClassLoader(new URL[]{javaApi}, ClassDowngrader.class.getClassLoader());
    private static final Map<Integer, VersionProvider> downgraders = collectProviders();

    public final int target;

    protected ClassDowngrader(int versionTarget) {
        this.target = versionTarget;
    }

    public static ClassDowngrader downgradeTo(int version) {
        if (Utils.getCurrentClassVersion() != version) {
            return new ClassDowngrader(version);
        } else {
            return currentVersionDowngrader;
        }
    }

    public static Map<Integer, VersionProvider> collectProviders() {
        Iterator<VersionProvider> providerIterator = ServiceLoader.load(VersionProvider.class, classLoader).iterator();
        Map<Integer, VersionProvider> downgraders = new HashMap<>();
        while (providerIterator.hasNext()) {
            VersionProvider provider = providerIterator.next();
            downgraders.put(provider.inputVersion, provider);
        }
        return downgraders;
    }

    private static URL getJavaApiFromShade() throws IOException {
        return ClassDowngrader.class.getResource("/META-INF/lib/java-api.jar");
    }

    private static URL getJavaApiFromSystemProperty() throws IOException {
        String api = System.getProperty("jvmdg.java-api");
        if (api == null) {
            return null;
        }
        try {
            return new File(api).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IOException(e);
        }
    }

    private static URL getJavaApiFromMaven() throws IOException {
        Package pkg = ClassDowngrader.class.getPackage();
        String version = pkg.getImplementationVersion();
        if (version.contains("SNAPSHOT")) {
            // retrieve maven metadata
            URL url = URI.create("https://maven.wagyourtail.xyz/snapshots/xyz/wagyourtail/jvmdowngrader/jvmdowngrader-java-api/" + version + "/maven-metadata.xml").toURL();
            // get actual latest
            try (InputStream in = url.openStream()) {
                XMLDecoder decoder = new XMLDecoder(in);
                Map<String, Object> metadata = (Map<String, Object>) decoder.readObject();
                String snapshotVersion = ((Map<String, Object>) ((Map<String, Object>) metadata.get("versioning")).get("snapshot")).get("timestamp") + "-" + ((Map<String, Object>) ((Map<String, Object>) metadata.get("versioning")).get("snapshot")).get("buildNumber");
                return URI.create("https://maven.wagyourtail.xyz/snapshots/xyz/wagyourtail/jvmdowngrader/jvmdowngrader-java-api/" + version + "/jvmdowngrader-java-api-" + version + "-" + snapshotVersion + ".jar").toURL();
            }
        } else {
            File file = Constants.DIR;
            file.mkdirs();
            file = new File(file, "java-api-" + version + ".jar");
            // if already exists, return that
            if (file.exists()) {
                return file.toURI().toURL();
            }

            URL url = URI.create("https://maven.wagyourtail.xyz/releases/xyz/wagyourtail/jvmdowngrader/jvmdowngrader-java-api/" + version + "/jvmdowngrader-java-api-" + version + ".jar").toURL();
            // download
            try (InputStream in = url.openStream()) {
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return file.toURI().toURL();
        }
    }

    private static URL findJavaApi() {
        try {
            Path tmp = Files.createTempFile("jvmdg-api", ".jar");
            URL url = getJavaApiFromSystemProperty();
            if (url == null) {
                url = getJavaApiFromShade();
            }
            if (url == null) {
                url = getJavaApiFromMaven();
            }
            try (InputStream in = url.openStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            return tmp.toUri().toURL();
        } catch (IOException e) {
            throw new RuntimeException("Failed to find java api", e);
        }
    }

    public Set<MemberNameAndDesc> getMembers(int version, Type type) throws IOException {
        for (int vers = version; vers > target; vers--) {
            VersionProvider downgrader = downgraders.get(vers);
            if (downgrader == null) {
                throw new RuntimeException("Unsupported class version: " + vers + " supported: " + downgraders.keySet());
            }
            downgrader.ensureInit();
            Type stubbed = downgrader.stubClass(type);
            if (!stubbed.equals(type)) {
                try (InputStream stream = classLoader.getResourceAsStream(stubbed.getInternalName() + ".class")) {
                    if (stream == null) throw new IOException("Failed to find stubbed class: " + stubbed);
                    ClassReader reader = new ClassReader(stream);
                    ClassNode node = new ClassNode();
                    reader.accept(node, ClassReader.SKIP_CODE);
                    Set<MemberNameAndDesc> members = new HashSet<>();
                    for (MethodNode o : node.methods) {
                        if ((o.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                        members.add(new MemberNameAndDesc(o.name, Type.getMethodType(o.desc)));
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
            if (stream == null) return null;
            ClassReader reader = new ClassReader(stream);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_CODE);
            Set<MemberNameAndDesc> members = new HashSet<>();
            for (MethodNode o : node.methods) {
                if ((o.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                members.add(new MemberNameAndDesc(o.name, Type.getMethodType(o.desc)));
            }
            return members;
        }
    }

    public List<Type> getSupertypes(int version, Type type) throws IOException {
        for (int vers = version; vers > target; vers--) {
            VersionProvider downgrader = downgraders.get(vers);
            if (downgrader == null) {
                throw new RuntimeException("Unsupported class version: " + vers + " supported: " + downgraders.keySet());
            }
            downgrader.ensureInit();
            Type stubbed = downgrader.stubClass(type);
            if (!stubbed.equals(type)) {
                try (InputStream stream = classLoader.getResourceAsStream(stubbed.getInternalName() + ".class")) {
                    if (stream == null) throw new IOException("Failed to find stubbed class: " + stubbed);
                    ClassReader reader = new ClassReader(stream);
                    List<Type> types = new ArrayList<>();
                    types.add(Type.getObjectType(reader.getSuperName()));
                    for (String anInterface : reader.getInterfaces()) {
                        types.add(Type.getObjectType(anInterface));
                    }
                    return types;
                }
            }
        }
        try (InputStream stream = classLoader.getResourceAsStream(type.getInternalName() + ".class")) {
            if (stream == null) return null;
            ClassReader reader = new ClassReader(stream);
            List<Type> types = new ArrayList<>();
            types.add(Type.getObjectType(reader.getSuperName()));
            for (String anInterface : reader.getInterfaces()) {
                types.add(Type.getObjectType(anInterface));
            }
            return types;
        }
    }

    public Type stubClass(int version, Type type) {
        for (int vers = version; vers > target; vers--) {
            VersionProvider downgrader = downgraders.get(vers);
            if (downgrader == null) {
                throw new RuntimeException("Unsupported class version: " + vers + " supported: " + downgraders.keySet());
            }
            downgrader.ensureInit();
            Type stubbed = downgrader.stubClass(type);
            if (!stubbed.equals(type)) {
                return stubbed;
            }
        }
        return type;
    }

    protected Set<ClassNode> downgrade(ClassNode clazz, boolean enableRuntime, Function<String, ClassNode> getReadOnly) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IOException {
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
                newClasses.add(downgrader.downgrade(c, newClasses, enableRuntime, getReadOnly));
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

    public Map<String, byte[]> downgrade(/* in out */ AtomicReference<String> name, byte[] bytes, boolean enableRuntime, final Function<String, byte[]> getExtraRead) throws IllegalClassFormatException {
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
        ClassNode node = bytesToClassNode(bytes);
        if (name.get() == null) {
            name.set(node.name);
        } else if (!name.get().equals(node.name)) {
            throw new RuntimeException("Class name mismatch: " + name.get() + " != " + node.name);
        }
        Map<String, byte[]> outputs = new HashMap<>();
        try {
            if (Constants.DEBUG) System.out.println("Transforming " + name.get());
            Set<ClassNode> extra = downgrade(node, enableRuntime, new Function<String, ClassNode>() {

                @Override
                public ClassNode apply(String s) {
                    try {
                        byte[] out = getExtraRead.apply(s);
                        if (out == null) {
                            return null;
                        }
                        return bytesToClassNode(out);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            for (ClassNode c : extra) {
                if (Constants.DEBUG) {
                    File f = new File(Constants.DEBUG_DIR, c.name + ".javasm");
                    f.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(f)) {
                        TraceClassVisitor tcv = new TraceClassVisitor(null, new Textifier(), new PrintWriter(fos));
                        c.accept(tcv);
                    } catch (IOException ignored) {
                    }
                }
                outputs.put(c.name, classNodeToBytes(c, getExtraRead));
            }
        } catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                 InstantiationException | IOException e) {
            throw new RuntimeException(e);
        }
        if (Constants.DEBUG) {
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                if (!entry.getKey().equals(name.get())) {
                    System.out.println("Downgraded " + entry.getKey() + " from unknown to " + target);
                } else {
                    System.out.println("Downgraded " + entry.getKey() + " from " + version + " to " + target);
                }
                writeBytesToDebug(entry.getKey(), entry.getValue());
            }
        }
        return outputs;
    }

    public byte[] classNodeToBytes(ClassNode node, final Function<String, byte[]> getExtraRead) {
        ASMClassWriter cw = new ASMClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, new Function<String, String>() {
            @Override
            public String apply(String s) {
                byte[] b = getExtraRead.apply(s);
                if (b == null) return null;
                ClassNode cn = bytesToClassNode(b, ClassReader.SKIP_CODE);
                return stubClass(cn.version, Type.getObjectType(cn.superName)).getInternalName();
            }
        });
        node.accept(cw);
        return cw.toByteArray();
    }

    public static ClassNode bytesToClassNode(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    public static ClassNode bytesToClassNode(byte[] bytes, int flags) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, flags);
        return node;
    }

    public void writeBytesToDebug(String name, byte[] bytes) {
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
        vp.ensureInit();
        return vp;
    }

    public static void main(String[] args) {
        //TODO
    }

}
