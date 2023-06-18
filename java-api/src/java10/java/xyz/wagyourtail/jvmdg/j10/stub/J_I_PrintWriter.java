package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.io.*;
import java.nio.charset.Charset;

public class J_I_PrintWriter {

    @Stub(opcVers = Opcodes.V10, ref = @Ref(value = "Ljava/io/PrintWriter", member = "<init>"))
    public static PrintWriter init(OutputStream out, boolean autoFlush, Charset encoding) {
        return new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, encoding)), autoFlush);
    }

}
