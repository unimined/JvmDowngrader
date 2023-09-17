package xyz.wagyourtail.jvmdg;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.classloader.DowngradingClassLoader;

import java.beans.XMLDecoder;
import java.io.*;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ClassDowngrader {
    public static final ClassDowngrader currentVersionDowngrader = new ClassDowngrader(VersionProvider.getCurrentClassVersion());
    public static final URL javaApi = findJavaApi();
    public static final DowngradingClassLoader classLoader = new DowngradingClassLoader(new URL[]{javaApi}, ClassDowngrader.class.getClassLoader());
    private static final Map<Integer, VersionProvider> downgraders = collectProviders();
    public final int target;

    protected ClassDowngrader(int versionTarget) {
        this.target = versionTarget;
    }

    public static ClassDowngrader downgradeTo(int version) {
        if (VersionProvider.getCurrentClassVersion() != version) {
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
            URL url = URI.create("https://maven.wagyourtail.xyz/snapshots/xyz/wagyourtail/unimined/jvmdowngrader-java-api/" + version + "/maven-metadata.xml").toURL();
            // get actual latest
            try (InputStream in = url.openStream()) {
                XMLDecoder decoder = new XMLDecoder(in);
                Map<String, Object> metadata = (Map<String, Object>) decoder.readObject();
                String snapshotVersion = ((Map<String, Object>) ((Map<String, Object>) metadata.get("versioning")).get("snapshot")).get("timestamp") + "-" + ((Map<String, Object>) ((Map<String, Object>) metadata.get("versioning")).get("snapshot")).get("buildNumber");
                return URI.create("https://maven.wagyourtail.xyz/snapshots/xyz/wagyourtail/unimined/jvmdowngrader-java-api/" + version + "/jvmdowngrader-java-api-" + version + "-" + snapshotVersion + ".jar").toURL();
            }
        } else {
            File file = Constants.DIR;
            file.mkdirs();
            file = new File(file, "java-api-" + version + ".jar");
            // if already exists, return that
            if (file.exists()) {
                return file.toURI().toURL();
            }

            URL url = URI.create("https://maven.wagyourtail.xyz/releases/xyz/wagyourtail/unimined/jvmdowngrader-java-api/" + version + "/jvmdowngrader-java-api-" + version + ".jar").toURL();
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

    protected Set<ClassNode> downgrade(ClassNode clazz, Function<String, ClassNode> getReadOnly) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InstantiationException {
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
                newClasses.add(downgrader.downgrade(c, newClasses, getReadOnly));
            }
            classes = newClasses;
            version = downgrader.outputVersion;
        }
        return classes;
    }

    public Map<String, byte[]> downgrade(/* in out */ AtomicReference<String> name, byte[] bytes, final Function<String, byte[]> getExtraRead) throws IllegalClassFormatException {
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
            Set<ClassNode> extra = downgrade(node, new Function<String, ClassNode>() {

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
                outputs.put(c.name, classNodeToBytes(c, getExtraRead));
            }
        } catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                 InstantiationException e) {
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
                return bytesToClassNode(b).superName;
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

    public static void main(String[] args) {
        //TODO
    }

    private static class ClassWriterASM extends ClassWriter {
        private final Function<String, byte[]> getExtraRead;

        public ClassWriterASM(int flags, Function<String, byte[]> getExtraRead) {
            super(flags);
            this.getExtraRead = getExtraRead;
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            try {
                return super.getCommonSuperClass(type1, type2);
            } catch (Exception e) {
                try {
                    List<String> tree1 = buildInheritanceTree(type1);
                    List<String> tree2 = buildInheritanceTree(type2);
                    // find first that's in both
                    for (String s : tree1) {
                        if (tree2.contains(s)) {
                            return s;
                        }
                    }
                    throw new IllegalStateException("they should both extend Object...");
                } catch (Exception ex) {
                    ex.addSuppressed(e);
                    throw ex;
                }
            }
        }

        protected List<String> buildInheritanceTree(String type) {
            List<String> tree = new ArrayList<>();
            tree.add(type);
            while (!type.equals("java/lang/Object")) {
                type = getSuperClass(type);
                tree.add(type);
            }
            tree.add("java/lang/Object");
            return tree;
        }

        protected String getSuperClass(String type) {
            // try loading
            try {
                Class<?> clazz = java.lang.Class.forName(type.replace('/', '.'));
                return clazz.getSuperclass().getCanonicalName().replace('.', '/');
            } catch (ClassNotFoundException ignored) {
                try {
                    ClassNode node = bytesToClassNode(getExtraRead.apply(type), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    return node.superName;
                } catch (Exception e) {
                    return "java/lang/Object";
                }
            }
        }
    }

}
