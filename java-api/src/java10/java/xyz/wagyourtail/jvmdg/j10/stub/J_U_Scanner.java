package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class J_U_Scanner {

    @Stub(ref = @Ref(value = "Ljava/util/Scanner;", member = "<init>"))
    public static Scanner init(ReadableByteChannel source, Charset charset) {
        return new Scanner(source, charset.name());
    }

}
