package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

public class J_N_F_Files {

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/nio/file/Files;"))
    public static String readString(Path path) throws IOException {
        return readString(path, StandardCharsets.UTF_8);
    }

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/nio/file/Files;"))
    public static String readString(Path path, Charset cs) throws IOException {
        return new String(Files.readAllBytes(path), cs);
    }

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/nio/file/Files;"))
    public static void writeString(Path path, CharSequence cs, OpenOption... options) throws IOException {
        writeString(path, cs, StandardCharsets.UTF_8, options);
    }

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/nio/file/Files;"))
    public static void writeString(Path path, CharSequence cs, Charset cs2, OpenOption... options) throws IOException {
        Files.write(path, cs.toString().getBytes(cs2), options);
    }

}
