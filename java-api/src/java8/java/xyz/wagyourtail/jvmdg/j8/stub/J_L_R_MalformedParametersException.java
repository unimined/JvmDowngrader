package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

@Stub(javaVersion = Opcodes.V1_8, ref = @Ref("Ljava/lang/reflect/MalformedParametersException;"))
public class J_L_R_MalformedParametersException extends RuntimeException {

    public J_L_R_MalformedParametersException() {
    }

    public J_L_R_MalformedParametersException(String message) {
        super(message);
    }

}
