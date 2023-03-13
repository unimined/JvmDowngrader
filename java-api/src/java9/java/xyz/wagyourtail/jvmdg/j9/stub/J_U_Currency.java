package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.Currency;

public class J_U_Currency {

    @Stub(javaVersion = Opcodes.V9)
    public static String getNumericCodeAsString(Currency self) {
        int code = self.getNumericCode();
        if (code < 100) {
            StringBuilder sb = new StringBuilder();
            sb.append('0');
            if (code < 10) {
                sb.append('0');
            }
            return sb.append(code).toString();
        }
        return Integer.toString(code);
    }

}
