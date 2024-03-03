package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.Clock;
import java.time.ZoneId;

public class J_T_Clock {

    @Stub
    public static Clock tickMillis(ZoneId zone) {
        return Clock.tick(Clock.system(zone), java.time.Duration.ofMillis(1));
    }

}
