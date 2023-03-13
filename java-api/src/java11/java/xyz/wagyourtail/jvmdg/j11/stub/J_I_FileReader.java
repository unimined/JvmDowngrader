package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.io.*;
import java.nio.charset.Charset;

@Stub(javaVersion = Opcodes.V11, ref = @Ref("Ljava/io/FileReader;"))
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
