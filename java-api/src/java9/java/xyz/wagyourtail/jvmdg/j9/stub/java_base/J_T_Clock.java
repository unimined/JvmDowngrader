package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.Clock;
import java.time.ZoneId;

public class J_T_Clock {

    @Stub(ref = @Ref("java/time/Clock"))
    public static Clock tickMillis(ZoneId zone) {
        return Clock.tick(Clock.system(zone), java.time.Duration.ofMillis(1));
    }

}
