package xyz.wagyourtail.jvmdg.standalone.classloader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.VersionProvider;
import xyz.wagyourtail.jvmdg.standalone.ClassDowngrader;

import java.io.*;
import java.util.Arrays;

public class DowngradingClassLoader extends ClassLoader {

    private final int targetVersion;
    private final ClassDowngrader downgrader;

    public DowngradingClassLoader(ClassLoader parent) throws ClassNotFoundException {
        super(parent);
        this.targetVersion = VersionProvider.getCurrentClassVersion();
        this.downgrader = new ClassDowngrader(targetVersion);
    }

    public DowngradingClassLoader(ClassLoader parent, int targetVersion) {
        super(parent);
        this.targetVersion = targetVersion;
        this.downgrader = new ClassDowngrader(targetVersion);
    }

    protected byte[] readAllBytes(InputStream stream) throws IOException {
        byte[] b = new byte[Integer.MAX_VALUE];
        int n = 0;
        while (n < Integer.MAX_VALUE) {
            int count = stream.read(b, n, Integer.MAX_VALUE - n);
            if (count < 0) {
                if (n == 0) {
                    return null;
                } else {
                    return Arrays.copyOf(b, n);
                }
            }
            n += count;
        }
        return b;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        // get the class bytes from the original class loader
        InputStream is = getResourceAsStream(name.replace('.', '/') + ".class");
        try {
            if (is == null) {
                throw new ClassNotFoundException(name);
            }
            if (!is.markSupported()) {
                is = new BufferedInputStream(is);
            }
            // mark and read to version byte
            is.mark(8);
            byte[] bytes = new byte[8];
            int read = is.read(bytes, 0, 8);
            if (read != 8) {
                throw new ClassNotFoundException(name);
            }
            // check magic
            if (bytes[0] != (byte) 0xCA || bytes[1] != (byte) 0xFE || bytes[2] != (byte) 0xBA || bytes[3] != (byte) 0xBE) {
                throw new ClassNotFoundException(name);
            }
            // ignore minor version
            // get major version
            int version = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
            if (version <= targetVersion) {
                // already at or below the target version
                return super.findClass(name);
            }
            // reset and read into a ClassReader
            is.reset();
            ClassReader cr = new ClassReader(is);
            // convert to class node
            ClassNode node = new ClassNode();
            cr.accept(node, 0);
            // apply the downgrade
            downgrader.downgrade(node);

            // write the class back out
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            node.accept(cw);
            // define the class
            bytes = cw.toByteArray();
            if (Constants.DEBUG) {
                System.out.println("Downgraded " + name + " from " + version + " to " + targetVersion);
                File f= new File(Constants.DEBUG_DIR, name.replace('.', '/') + ".class");
                f.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    fos.write(bytes);
                }
            }
            return defineClass(name, bytes, 0, bytes.length);
        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                System.err.println("Unable to close class input stream: ");
                e.printStackTrace();
                // ignore
            }
        }
    }

}
