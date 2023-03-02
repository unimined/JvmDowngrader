package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class J_N_F_Path {

    @Stub(value = JavaVersion.VERSION_11, desc = "Ljava/nio/file/Path;")
    public static Path of(String first, String... more) {
        return Paths.get(first, more);
    }

    @Stub(value = JavaVersion.VERSION_11, desc = "Ljava/nio/file/Path;")
    public static Path of(URI uri) {
        return Paths.get(uri);
    }

}
