package xyz.wagyourtail.jvmdg.j24.stub;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class J_L_Process {

    @Stub
    public static boolean waitFor(Process self, Duration duration) throws InterruptedException {
        Objects.requireNonNull(duration);
        return self.waitFor(TimeUnit.NANOSECONDS.convert(duration), TimeUnit.NANOSECONDS);
    }

}
