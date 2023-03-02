package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.UnsafeAccess;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.Charset;

@Stub(value = JavaVersion.VERSION_11, desc = "Ljava/io/FileWriter;")
public class J_I_FileWriter extends OutputStreamWriter {

    public J_I_FileWriter(String fileName) throws FileNotFoundException {
        super(new FileOutputStream(fileName));
    }

    public J_I_FileWriter(String fileName, boolean append) throws FileNotFoundException {
        super(new FileOutputStream(fileName, append));
    }

    public J_I_FileWriter(File file) throws FileNotFoundException {
        super(new FileOutputStream(file));
    }

    public J_I_FileWriter(File file, boolean append) throws FileNotFoundException {
        super(new FileOutputStream(file, append));
    }

    public J_I_FileWriter(FileDescriptor fd) {
        super(new FileOutputStream(fd));
    }

    public J_I_FileWriter(String fileName, Charset charset) throws IOException {
        super(new FileOutputStream(fileName), charset);
    }

    public J_I_FileWriter(String fileName, Charset charset, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append), charset);
    }

    public J_I_FileWriter(File file, Charset charset) throws IOException {
        super(new FileOutputStream(file), charset);
    }

    public J_I_FileWriter(File file, Charset charset, boolean append) throws IOException {
        super(new FileOutputStream(file, append), charset);
    }

}
