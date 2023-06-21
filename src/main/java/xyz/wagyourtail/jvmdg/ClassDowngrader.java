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
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ClassDowngrader {
    public static final ClassDowngrader currentVersionDowngrader = new ClassDowngrader(VersionProvider.getCurrentClassVersion());
    public static final DowngradingClassLoader classLoader = new DowngradingClassLoader(new URL[]{findJavaApi()}, ClassDowngrader.class.getClassLoader());
    private static final Map<Integer, VersionProvider> downgraders = new HashMap<>();
    private final int target;

    static {
        collectProviders();
    }

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

    public static void collectProviders() {
        Iterator<VersionProvider> providerIterator = ServiceLoader.load(VersionProvider.class, classLoader).iterator();
        while (providerIterator.hasNext()) {
            VersionProvider provider = providerIterator.next();
            downgraders.put(provider.inputVersion, provider);
        }
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
            URL url = getJavaApiFromSystemProperty();
            if (url != null) {
                return url;
            }
            url = getJavaApiFromShade();
            if (url != null) {
                return url;
            }
            return getJavaApiFromMaven();
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
                throw new RuntimeException("Unsupported class version: " + version);
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

    public Map<String, byte[]> downgrade(String name, byte[] bytes, final Function<String, byte[]> getExtraRead) throws IllegalClassFormatException {
        // check magic
        if (bytes[0] != (byte) 0xCA || bytes[1] != (byte) 0xFE || bytes[2] != (byte) 0xBA ||
            bytes[3] != (byte) 0xBE) {
            throw new IllegalClassFormatException(name);
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
        Map<String, byte[]> outputs = new HashMap<>();
        try {
            if (Constants.DEBUG) System.out.println("Transforming " + name);
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
                outputs.put(c.name, classNodeToBytes(c));
            }
        } catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
        if (Constants.DEBUG) {
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                if (!entry.getKey().equals(name)) {
                    System.out.println("Downgraded " + entry.getKey() + " from unknown to " + target);
                } else {
                    System.out.println("Downgraded " + entry.getKey() + " from " + version + " to " + target);
                }
                writeBytesToDebug(entry.getKey(), entry.getValue());
            }
        }
        return outputs;
    }

    public byte[] classNodeToBytes(ClassNode node) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(cw);
        return cw.toByteArray();
    }

    public ClassNode bytesToClassNode(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
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

}
