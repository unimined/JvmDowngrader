package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class J_U_C_TimeUnit {

    @Stub(javaVersion = Opcodes.V11)
    public static long convert(TimeUnit tu, Duration duration) {
        return tu.convert(duration.getSeconds(), TimeUnit.SECONDS) + tu.convert(
            duration.getNano(),
            TimeUnit.NANOSECONDS
        );
    }

}
