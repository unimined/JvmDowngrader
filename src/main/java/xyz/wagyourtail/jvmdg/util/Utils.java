package xyz.wagyourtail.jvmdg.util;

import sun.misc.Unsafe;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.*;
import java.util.Map;

public class Utils {

    public static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodHandles.Lookup getImplLookup() {
        try {
            // ensure lookup is initialized
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            // get the impl_lookup field
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Unsafe unsafe = getUnsafe();
            MethodHandles.Lookup IMPL_LOOKUP;
            IMPL_LOOKUP = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, unsafe.staticFieldOffset(implLookupField));
            if (IMPL_LOOKUP != null) return IMPL_LOOKUP;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        throw new UnsupportedOperationException("Unable to get MethodHandles.Lookup.IMPL_LOOKUP");
    }

    public static FileSystem openZipFileSystem(Path path, Map<String,Object> options) throws IOException {
        if (options.containsKey("create")) {
            if (options.get("create") == Boolean.TRUE) {
                options.put("create", "true");
            }
        }
        return FileSystems.newFileSystem(URI.create("jar:" + path.toUri()), options, null);
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        int readBytes = 0;
        byte[] bytes = new byte[8192];
        // read into bytes
        int read;
        while ((read = in.read(bytes, readBytes, bytes.length - readBytes)) != -1) {
            readBytes += read;
            if (readBytes == bytes.length) {
                byte[] old = bytes;
                bytes = new byte[readBytes << 1];
                System.arraycopy(old, 0, bytes, 0, readBytes);
            }
        }
        if (readBytes == bytes.length) return bytes;
        byte[] trimmed = new byte[readBytes];
        System.arraycopy(bytes, 0, trimmed, 0, readBytes);
        return trimmed;
    }

    public static int getCurrentClassVersion() {
        String version = System.getProperty("java.class.version");
        if (version != null) {
            try {
                return Integer.parseInt(version.split("\\.")[0]);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        throw new UnsupportedOperationException("Unable to determine current class version");
    }
}
