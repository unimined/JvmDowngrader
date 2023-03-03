package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.time.Clock;
import java.time.ZoneId;

public class J_T_Clock {

    @Stub(JavaVersion.VERSION_1_9)
    public static Clock tickMillis(ZoneId zone) {
        return Clock.tick(Clock.system(zone), java.time.Duration.ofMillis(1));
    }
}
