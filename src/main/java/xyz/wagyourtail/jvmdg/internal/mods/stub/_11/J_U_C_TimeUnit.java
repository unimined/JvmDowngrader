package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class J_U_C_TimeUnit {

    @Stub(JavaVersion.VERSION_11)
    public static long convert(TimeUnit tu, Duration duration) {
        return tu.convert(duration.getSeconds(), TimeUnit.SECONDS) + tu.convert(duration.getNano(), TimeUnit.NANOSECONDS);
    }
}
