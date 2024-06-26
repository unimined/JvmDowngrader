package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

public class J_I_Reader {

    @Stub
    public static long transferTo(Reader reader, Writer writer) throws IOException {
        Objects.requireNonNull(writer, "target");
        long transferred = 0L;
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer)) > -1) {
            writer.write(buffer, 0, read);
            transferred += read;
        }
        return transferred;
    }

}
