package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.time.Clock;
import java.time.ZoneId;

public class J_T_Clock {

    @Stub(opcVers = Opcodes.V9)
    public static Clock tickMillis(ZoneId zone) {
        return Clock.tick(Clock.system(zone), java.time.Duration.ofMillis(1));
    }

}
