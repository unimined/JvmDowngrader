package xyz.wagyourtail.jvmdg.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.Map;

public class Utils {

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

}
