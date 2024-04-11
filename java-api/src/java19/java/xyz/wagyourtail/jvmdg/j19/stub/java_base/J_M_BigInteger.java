package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.math.BigInteger;

public class J_M_BigInteger {

    @Stub
    public static BigInteger parallelMultiply(BigInteger a, BigInteger b) {
        return a.multiply(b); // TODO: probably should actually parallelize this
    }

}
