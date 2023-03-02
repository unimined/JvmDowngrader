package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.*;
import java.nio.charset.Charset;

@Stub(value = JavaVersion.VERSION_11, desc = "Ljava/io/FileReader;")
public class J_I_FileReader extends InputStreamReader {

    public J_I_FileReader(String fileName) throws FileNotFoundException {
        super(new FileInputStream(fileName));
    }

    public J_I_FileReader(File file) throws FileNotFoundException {
        super(new FileInputStream(file));
    }

    public J_I_FileReader(FileDescriptor fd) {
        super(new FileInputStream(fd));
    }

    public J_I_FileReader(String fileName, Charset charset) throws IOException {
        super(new FileInputStream(fileName), charset);
    }

    public J_I_FileReader(File file, Charset charset) throws IOException {
        super(new FileInputStream(file), charset);
    }
}
