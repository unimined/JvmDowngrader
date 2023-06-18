package xyz.wagyourtail.jvmdg.j12.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class J_N_F_Files {

    @Stub(opcVers = Opcodes.V12, ref = @Ref("Ljava/nio/file/Files;"))
    public static long mismatch(Path p1, Path p2) {
        try (InputStream f1 = Files.newInputStream(p1); InputStream f2 = Files.newInputStream(p2)) {
            long pos = 0;
            int b1, b2;
            while ((b1 = f1.read()) != -1 & (b2 = f2.read()) != -1) {
                if (b1 != b2) {
                    return pos;
                }
                pos++;
            }
            if (b1 == -1 && b2 == -1) {
                return -1;
            }
            return pos;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
