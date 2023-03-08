package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.math.BigInteger;
import java.util.Arrays;

public class J_M_BigInteger {

    @Stub(javaVersion = Opcodes.V9, ref = @Ref(value = "Ljava/math/BigInteger", member = "<init>"))
    public static BigInteger init(byte[] val, int off, int len) {
        return new BigInteger(Arrays.copyOfRange(val, off, off + len));
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref(value = "Ljava/math/BigInteger", member = "<init>"))
    public static BigInteger init(int signum, byte[] magnitude, int off, int len) {
        return new BigInteger(signum, Arrays.copyOfRange(magnitude, off, off + len));
    }

}
