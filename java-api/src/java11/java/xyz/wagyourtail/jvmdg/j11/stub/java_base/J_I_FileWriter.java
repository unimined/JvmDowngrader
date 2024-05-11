package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Adapter;

import java.io.*;
import java.nio.charset.Charset;

@Adapter("Ljava/io/FileWriter;")
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
