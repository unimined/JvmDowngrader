package xyz.wagyourtail.jvmdg.standalone;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.VersionProvider;
import xyz.wagyourtail.jvmdg.j10.Java10Downgrader;
import xyz.wagyourtail.jvmdg.j11.Java11Downgrader;
import xyz.wagyourtail.jvmdg.j12.Java12Downgrader;
import xyz.wagyourtail.jvmdg.j13.Java13Downgrader;
import xyz.wagyourtail.jvmdg.j14.Java14Downgrader;
import xyz.wagyourtail.jvmdg.j15.Java15Downgrader;
import xyz.wagyourtail.jvmdg.j16.Java16Downgrader;
import xyz.wagyourtail.jvmdg.j17.Java17Downgrader;
import xyz.wagyourtail.jvmdg.j9.Java9Downgrader;
import xyz.wagyourtail.jvmdg.standalone.classloader.DowngradingClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ClassDowngrader {

    private static final DowngradingClassLoader classLoader;

    private static final Map<Integer, VersionProvider> versionDowngraders = new HashMap<>();

    static {
        ClassLoader parent = ClassDowngrader.class.getClassLoader();
        if (parent instanceof DowngradingClassLoader) {
            classLoader = (DowngradingClassLoader) parent;
        } else {
            try {
                classLoader = new DowngradingClassLoader(parent);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        Thread.currentThread().setContextClassLoader(classLoader);
        versionDowngraders.put(Opcodes.V9, new Java9Downgrader());
        versionDowngraders.put(Opcodes.V10, new Java10Downgrader());
        versionDowngraders.put(Opcodes.V11, new Java11Downgrader());
        versionDowngraders.put(Opcodes.V12, new Java12Downgrader());
        versionDowngraders.put(Opcodes.V13, new Java13Downgrader());
        versionDowngraders.put(Opcodes.V14, new Java14Downgrader());
        versionDowngraders.put(Opcodes.V15, new Java15Downgrader());
        versionDowngraders.put(Opcodes.V16, new Java16Downgrader());
        versionDowngraders.put(Opcodes.V17, new Java17Downgrader());

    }

    private int target;

    public ClassDowngrader(int versionTarget) {
        this.target = versionTarget;
    }

    @SuppressWarnings("IOStreamConstructor")
    public void downgradeZip(File zip, File output) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().endsWith(".class")) {
                        ClassNode clazz = new ClassNode();
                        new ClassReader(zis).accept(clazz, 0);
                        downgrade(clazz);
                        ZipEntry newEntry = new ZipEntry(entry.getName());
                        newEntry.setTime(entry.getTime());
                        zos.putNextEntry(newEntry);
                        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                        try {
                            clazz.accept(writer);
                        } catch (Throwable t) {
                            System.err.println("Error while writing class " + clazz.name);
                            throw t;
                        }
                        zos.write(writer.toByteArray());
                        zos.closeEntry();
                    } else {
                        zos.putNextEntry(entry);
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    }
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                output.delete();
                throw new RuntimeException(e);
            }
        }
    }

    public void downgrade(ClassNode clazz) throws InvocationTargetException, IllegalAccessException {
        while (clazz.version > target) {
            VersionProvider provider = versionDowngraders.get(clazz.version);
            if (provider == null) {
                throw new UnsupportedOperationException("Unable to downgrade class version " + clazz.version);
            }
            provider.downgrade(clazz);
        }
    }


}
