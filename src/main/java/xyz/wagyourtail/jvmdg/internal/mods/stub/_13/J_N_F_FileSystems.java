package xyz.wagyourtail.jvmdg.internal.mods.stub._13;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

public class J_N_F_FileSystems {

    @Stub(value = JavaVersion.VERSION_13, desc = "Ljava/nio/file/FileSystems;")
    public static FileSystem newFileSystem(Path p) throws IOException {
        return FileSystems.newFileSystem(p, (ClassLoader) null);
    }

    @Stub(value = JavaVersion.VERSION_13, desc = "Ljava/nio/file/FileSystems;")
    public static FileSystem newFileSystem(Path p, Map<String, ?> env) throws IOException {
        return FileSystems.newFileSystem(p, env, null);
    }

    @Stub(value = JavaVersion.VERSION_13, desc = "Ljava/nio/file/FileSystems;")
    public static FileSystem newFileSystem(Path p, Map<String, ?> env, ClassLoader loader) throws IOException {
        Objects.requireNonNull(p, "path");

        for (FileSystemProvider pro : FileSystemProvider.installedProviders()) {
            try {
                return pro.newFileSystem(p, env);
            } catch (UnsupportedOperationException e) {
            }
        }

        if (loader != null) {
            for (FileSystemProvider pro : ServiceLoader.load(FileSystemProvider.class, loader)) {
                try {
                    return pro.newFileSystem(p, env);
                } catch (UnsupportedOperationException e) {
                }
            }
        }

        throw new ProviderNotFoundException("Provider not found");
    }
}
