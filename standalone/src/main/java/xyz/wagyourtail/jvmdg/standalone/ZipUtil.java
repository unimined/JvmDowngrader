package xyz.wagyourtail.jvmdg.standalone;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

public class ZipUtil {

    public static FileSystem openZipFileSystem(Path path, Map<String,Object> options) throws IOException {
        if (options.containsKey("create")) {
            if (options.get("create") == Boolean.TRUE) {
                options.put("create", "true");
            }
        }
        return FileSystems.newFileSystem(URI.create("jar:" + path.toUri()), options, null);
    }


}
