package xyz.wagyourtail.jvmdg.j23.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.Duration;
import java.time.Instant;

public class J_T_Instant {

    @Stub
    public static Duration until(Instant instant, Instant endExclusive) {
        return Duration.between(instant, endExclusive);
    }

}
