package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Currency;

public class J_U_Currency {

    @Stub(JavaVersion.VERSION_1_9)
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
