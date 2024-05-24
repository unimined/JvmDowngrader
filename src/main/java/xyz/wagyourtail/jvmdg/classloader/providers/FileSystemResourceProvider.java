package xyz.wagyourtail.jvmdg.classloader.providers;

import xyz.wagyourtail.jvmdg.classloader.ResourceProvider;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class FileSystemResourceProvider implements ResourceProvider {
    private final FileSystem jarFile;

    public FileSystemResourceProvider(FileSystem jarFile) {
        this.jarFile = jarFile;
    }


    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> urls = new ArrayList<>();
        for (Path rootDirectory : jarFile.getRootDirectories()) {
            Path resource = rootDirectory.resolve(name);
            if (Files.exists(resource)) {
                urls.add(resource.toUri().toURL());
            }
        }
        return Collections.enumeration(urls);
    }

    @Override
    public void close() throws IOException {
        jarFile.close();
    }

}
