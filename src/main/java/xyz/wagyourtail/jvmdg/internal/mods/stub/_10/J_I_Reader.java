package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

public class J_I_Reader {

    @Stub(value = JavaVersion.VERSION_1_10, subtypes = true)
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
