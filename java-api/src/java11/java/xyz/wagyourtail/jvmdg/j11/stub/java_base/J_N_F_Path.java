package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class J_N_F_Path {

    @Stub(ref = @Ref("Ljava/nio/file/Path;"))
    public static Path of(String first, String... more) {
        return Paths.get(first, more);
    }

    @Stub(ref = @Ref("Ljava/nio/file/Path;"))
    public static Path of(URI uri) {
        return Paths.get(uri);
    }

}
