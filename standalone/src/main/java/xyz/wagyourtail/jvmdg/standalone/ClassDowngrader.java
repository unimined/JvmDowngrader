package xyz.wagyourtail.jvmdg.standalone;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.Function;
import xyz.wagyourtail.jvmdg.VersionProvider;
import xyz.wagyourtail.jvmdg.standalone.classloader.DowngradingClassLoader;

import java.io.*;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ClassDowngrader {
    private static final Map<Integer, String> versionDowngraders = new HashMap<>();
    public static final DowngradingClassLoader classLoader;

    static {
//        versionDowngraders.put(Opcodes.V1_8, "xyz.wagyourtail.jvmdg.j8.Java8Downgrader");
        versionDowngraders.put(Opcodes.V9, "xyz.wagyourtail.jvmdg.j9.Java9Downgrader");
        versionDowngraders.put(Opcodes.V10, "xyz.wagyourtail.jvmdg.j10.Java10Downgrader");
        versionDowngraders.put(Opcodes.V11, "xyz.wagyourtail.jvmdg.j11.Java11Downgrader");
        versionDowngraders.put(Opcodes.V12, "xyz.wagyourtail.jvmdg.j12.Java12Downgrader");
        versionDowngraders.put(Opcodes.V13, "xyz.wagyourtail.jvmdg.j13.Java13Downgrader");
        versionDowngraders.put(Opcodes.V14, "xyz.wagyourtail.jvmdg.j14.Java14Downgrader");
        versionDowngraders.put(Opcodes.V15, "xyz.wagyourtail.jvmdg.j15.Java15Downgrader");
        versionDowngraders.put(Opcodes.V16, "xyz.wagyourtail.jvmdg.j16.Java16Downgrader");
        versionDowngraders.put(Opcodes.V17, "xyz.wagyourtail.jvmdg.j17.Java17Downgrader");
        classLoader = new DowngradingClassLoader(new URL[]{findJavaApi()}, ClassDowngrader.class.getClassLoader());
    }

    private static final Map<Integer, VersionProvider> downgraders = new HashMap<>();

    private final int target;

    public ClassDowngrader(int versionTarget) {
        this.target = versionTarget;
    }

    private static URL findJavaApi() {
        try {
            InputStream stream = ClassDowngrader.class.getResourceAsStream("/META-INF/lib/java-api.jar");
            if (stream == null) {
                // get from java properties
                String api = System.getProperty("jvmdg.java-api");
                if (api == null) {
                    throw new RuntimeException("Could not find java-api.jar");
                }
                    return new File(api).toURI().toURL();
            } else {
                // copy to temp file
                File temp = new File(Constants.DIR, "java-api.jar");
                if (temp.exists()) temp.delete();
                try (FileOutputStream fos = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = stream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                return temp.toURI().toURL();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("IOStreamConstructor")
    public void downgradeZip(File zip, File output) throws IOException {
        try (final FileSystem zipfs = ZipUtil.openZipFileSystem(zip.toPath(), new HashMap<String, Object>())) {
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output))) {
                Files.walkFileTree(zipfs.getPath("/"), new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (dir.startsWith("META-INF"))
                            return FileVisitResult.SKIP_SUBTREE;
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.getFileName().toString().endsWith(".class")) {
                            ClassNode clazz = new ClassNode();
                            try (InputStream is = Files.newInputStream(file)) {
                                new ClassReader(is).accept(clazz, 0);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            try {
                                Set<ClassNode> outputs = downgrade(clazz, new Function<String, ClassNode>() {
                                    @Override
                                    public ClassNode apply(String s) {
                                        try {
                                            Path path = zipfs.getPath(s + ".class");
                                            if (Files.exists(path)) {
                                                ClassNode node = new ClassNode();
                                                try (InputStream is = Files.newInputStream(path)) {
                                                    new ClassReader(is).accept(node, 0);
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
                                                return node;
                                            }
                                            return null;
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                                for (ClassNode node : outputs) {
                                    ZipEntry newEntry = new ZipEntry(node.name + ".class");
                                    newEntry.setTime(attrs.lastModifiedTime().toMillis());
                                    zos.putNextEntry(newEntry);
                                    zos.write(classNodeToBytes(node));
                                    zos.closeEntry();
                                }
                            } catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException |
                                     NoSuchMethodException | InstantiationException e) {
                                throw new IOException("Failed to downgrade class", e);
                            }
                        } else {
                            ZipEntry newEntry = new ZipEntry(file.toString());
                            newEntry.setTime(attrs.lastModifiedTime().toMillis());
                            zos.putNextEntry(newEntry);
                            Files.copy(file, zos);
                            zos.flush();
                            zos.closeEntry();
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        throw new IOException("Failed to visit file", exc);
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    public Set<ClassNode> downgrade(ClassNode clazz, Function<String, ClassNode> getReadOnly) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InstantiationException {
        Set<ClassNode> classes = new HashSet<>();
        classes.add(clazz);
        while (clazz.version > target) {
            int i = clazz.version;
            if (!downgraders.containsKey(i)) {
                synchronized (downgraders) {
                    if (!downgraders.containsKey(i)) {
                        if (!versionDowngraders.containsKey(i)) {
                            throw new RuntimeException("Unsupported class version: " + i);
                        }
                        VersionProvider p = (VersionProvider) Class.forName(versionDowngraders.get(i), true, classLoader).getConstructor().newInstance();
                        downgraders.put(i, p);
                    }
                }
            }
            Set<ClassNode> newClasses = new HashSet<>();
            VersionProvider downgrader = downgraders.get(i);
            for (ClassNode c : classes) {
                newClasses.add(downgrader.downgrade(c, newClasses, getReadOnly));
            }
            classes = newClasses;
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
                if (entry.getKey().equals(name)) {
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

    public static void main(String[] args) {
        //TODO
    }

}
