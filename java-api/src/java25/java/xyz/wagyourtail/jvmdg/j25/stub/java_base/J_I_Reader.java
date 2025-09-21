package xyz.wagyourtail.jvmdg.j25.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.*;
import java.util.List;

public class J_I_Reader {

    @Stub
    public static String readAllAsString(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        reader.transferTo(writer);
        return writer.toString();
    }

    @Stub
    public static List<String> readAllLines(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        try {
            return bufferedReader.lines().toList();
        } catch (UncheckedIOException uio) {
            throw new IOException(uio);
        }
    }

}
