package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Type;

public class J_L_R_Type {

    @Stub
    public static String getTypeName(Type self) {
        return self.toString();
    }

}
