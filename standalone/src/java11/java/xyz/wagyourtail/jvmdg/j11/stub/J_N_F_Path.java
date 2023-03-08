package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class J_N_F_Path {

    @Stub(javaVersion = Opcodes.V11, ref = @Ref("Ljava/nio/file/Path;"))
    public static Path of(String first, String... more) {
        return Paths.get(first, more);
    }

    @Stub(javaVersion = Opcodes.V11, ref = @Ref("Ljava/nio/file/Path;"))
    public static Path of(URI uri) {
        return Paths.get(uri);
    }

}
