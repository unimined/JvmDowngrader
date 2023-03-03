package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.math.BigInteger;
import java.util.Arrays;

public class J_M_BigInteger {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/math/BigInteger;<init>")
    public static BigInteger init(byte[] val, int off, int len) {
        return new BigInteger(Arrays.copyOfRange(val, off, off + len));
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/math/BigInteger;<init>")
    public static BigInteger init(int signum, byte[] magnitude, int off, int len) {
        return new BigInteger(signum, Arrays.copyOfRange(magnitude, off, off + len));
    }
}
