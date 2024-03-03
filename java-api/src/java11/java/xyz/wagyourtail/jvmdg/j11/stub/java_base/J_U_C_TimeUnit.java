package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class J_U_C_TimeUnit {

    @Stub
    public static long convert(TimeUnit tu, Duration duration) {
        return tu.convert(duration.getSeconds(), TimeUnit.SECONDS) + tu.convert(
            duration.getNano(),
            TimeUnit.NANOSECONDS
        );
    }

}
