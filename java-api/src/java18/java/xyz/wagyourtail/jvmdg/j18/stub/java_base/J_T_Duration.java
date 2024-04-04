package xyz.wagyourtail.jvmdg.j18.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.Duration;

public class J_T_Duration {

    @Stub
    public static boolean isPositive(Duration duration) {
        return !duration.isNegative() && !duration.isZero();
    }

}
