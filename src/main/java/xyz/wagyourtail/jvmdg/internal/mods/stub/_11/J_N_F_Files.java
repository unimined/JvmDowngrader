package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

public class J_N_F_Files {

    @Stub(value = JavaVersion.VERSION_11, desc = "Ljava/nio/file/Files;")
    public static String readString(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Stub(value = JavaVersion.VERSION_11, desc = "Ljava/nio/file/Files;")
    public static String readString(Path path, Charset cs) throws IOException {
        return new String(Files.readAllBytes(path), cs);
    }

    @Stub(value = JavaVersion.VERSION_11, desc = "Ljava/nio/file/Files;")
    public static void writeString(Path path, CharSequence cs, OpenOption... options) throws IOException {
        Files.writeString(path, cs, StandardCharsets.UTF_8, options);
    }

    @Stub(value = JavaVersion.VERSION_11, desc = "Ljava/nio/file/Files;")
    public static void writeString(Path path, CharSequence cs, Charset cs2, OpenOption... options) throws IOException {
        Files.write(path, cs.toString().getBytes(cs2), options);
    }
}
