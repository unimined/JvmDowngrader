package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

@Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/lang/invoke/LambdaConversionException;"))
public class J_L_I_LambdaConversionException extends Exception {

    public J_L_I_LambdaConversionException() {
    }

    public J_L_I_LambdaConversionException(String message) {
        super(message);
    }

    public J_L_I_LambdaConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public J_L_I_LambdaConversionException(Throwable cause) {
        super(cause);
    }

    public J_L_I_LambdaConversionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
