package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

public class J_I_Reader {

    @Stub(opcVers = Opcodes.V10, subtypes = true)
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
