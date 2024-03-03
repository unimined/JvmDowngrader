package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Currency;

public class J_U_Currency {

    @Stub
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
